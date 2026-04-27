package org.tenny.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.tenny.config.RagProperties;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Minimal RAG: load markdown from classpath, chunk by paragraph, score by character / 2-gram overlap with the query.
 * Injects into system prompt behind {@link #RAG_BEGIN_MARKER}; use {@link #stripRagFromContent(String)} before persisting.
 */
@Service
@Slf4j
public class RagService {

    /** Not present in base prompts; used to trim augmented system before saving sessions. */
    public static final String RAG_BEGIN_MARKER = "\n\n<<<RAG_CONTEXT>>>\n";

    private final RagProperties ragProperties;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private final List<TextChunk> chunks = new ArrayList<TextChunk>();

    public RagService(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    @PostConstruct
    public void loadCorpus() throws IOException {
        chunks.clear();
        if (!ragProperties.isEnabled()) {
            log.info("[RAG] disabled (rag.enabled=false)");
            return;
        }
        String pattern = ragProperties.getCorpusPattern();
        Resource[] resources = resolver.getResources(pattern);
        int files = 0;
        for (Resource res : resources) {
            if (!res.exists() || !res.isReadable()) {
                continue;
            }
            String name = res.getFilename() != null ? res.getFilename() : res.getDescription();
            String text = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
            indexDocument(name, text);
            files++;
        }
        log.info("[RAG] loaded {} chunks from {} resources (pattern={})", Integer.valueOf(chunks.size()),
                Integer.valueOf(files), pattern);
    }

    private void indexDocument(String sourceName, String fullText) {
        String normalized = fullText.replace("\r\n", "\n").trim();
        if (normalized.isEmpty()) {
            return;
        }
        String[] paras = normalized.split("\n\\s*\n");
        int p = 0;
        for (String para : paras) {
            para = para.trim().replaceAll("^[#\\s]+", "").trim();
            if (para.isEmpty()) {
                continue;
            }
            int max = Math.max(200, ragProperties.getMaxChunkChars());
            if (para.length() <= max) {
                chunks.add(new TextChunk(sourceName + "#" + p, para));
            } else {
                int start = 0;
                int part = 0;
                while (start < para.length()) {
                    int end = Math.min(para.length(), start + max);
                    chunks.add(new TextChunk(sourceName + "#" + p + "." + part, para.substring(start, end)));
                    start = end;
                    part++;
                }
            }
            p++;
        }
    }

    /**
     * Append RAG block to the first system message's content (must already be the base prompt).
     */
    public void augmentAgentSystem(List<Map<String, Object>> messages, String query) {
        if (!ragProperties.isEnabled() || chunks.isEmpty() || messages.isEmpty()) {
            return;
        }
        Map<String, Object> first = messages.get(0);
        if (!"system".equals(String.valueOf(first.get("role")))) {
            return;
        }
        Object c = first.get("content");
        if (!(c instanceof String)) {
            return;
        }
        String base = (String) c;
        String block = buildRagBlock(query);
        if (block.isEmpty()) {
            return;
        }
        first.put("content", base + RAG_BEGIN_MARKER + block);
    }

    public void augmentChatSystem(List<Map<String, String>> messages, String query) {
        if (!ragProperties.isEnabled() || chunks.isEmpty() || messages.isEmpty()) {
            return;
        }
        Map<String, String> first = messages.get(0);
        if (!"system".equals(first.get("role"))) {
            return;
        }
        String base = first.get("content");
        if (base == null) {
            base = "";
        }
        String block = buildRagBlock(query);
        if (block.isEmpty()) {
            return;
        }
        first.put("content", base + RAG_BEGIN_MARKER + block);
    }

    public String stripRagFromContent(String content) {
        if (content == null) {
            return null;
        }
        int i = content.indexOf(RAG_BEGIN_MARKER);
        if (i < 0) {
            return content;
        }
        return content.substring(0, i);
    }

    public void stripRagFromAgentMessages(List<Map<String, Object>> messages) {
        if (messages.isEmpty()) {
            return;
        }
        Map<String, Object> first = messages.get(0);
        if (!"system".equals(String.valueOf(first.get("role")))) {
            return;
        }
        Object c = first.get("content");
        if (c instanceof String) {
            first.put("content", stripRagFromContent((String) c));
        }
    }

    public void stripRagFromChatMessages(List<Map<String, String>> messages) {
        if (messages.isEmpty()) {
            return;
        }
        Map<String, String> first = messages.get(0);
        if (!"system".equals(first.get("role"))) {
            return;
        }
        first.put("content", stripRagFromContent(first.get("content")));
    }

    private String buildRagBlock(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }
        int k = Math.max(1, ragProperties.getTopK());
        List<ScoredChunk> ranked = rank(query.trim(), k);
        if (ranked.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("以下片段来自内部知识库，请优先用于回答政策、术语、流程类问题；")
                .append("涉及具体运单轨迹/实时状态时必须使用工具查询，勿凭片段编造运单事实。\n");
        for (int i = 0; i < ranked.size(); i++) {
            ScoredChunk sc = ranked.get(i);
            sb.append("\n--- 片段 ").append(i + 1).append(" [").append(sc.chunk.sourceId).append("] ---\n");
            sb.append(sc.chunk.text).append("\n");
        }
        return sb.toString();
    }

    private List<ScoredChunk> rank(String query, int topK) {
        Set<String> terms = queryTerms(query);
        if (terms.isEmpty()) {
            return Collections.emptyList();
        }
        List<ScoredChunk> scored = new ArrayList<ScoredChunk>();
        for (TextChunk ch : chunks) {
            long score = scoreChunk(ch.text, terms);
            if (score > 0) {
                scored.add(new ScoredChunk(ch, score));
            }
        }
        scored.sort(new Comparator<ScoredChunk>() {
            @Override
            public int compare(ScoredChunk a, ScoredChunk b) {
                int cmp = Long.compare(b.score, a.score);
                if (cmp != 0) {
                    return cmp;
                }
                return a.chunk.sourceId.compareTo(b.chunk.sourceId);
            }
        });
        if (scored.size() > topK) {
            return scored.subList(0, topK);
        }
        return scored;
    }

    private static Set<String> queryTerms(String query) {
        Set<String> terms = new LinkedHashSet<String>();
        for (int i = 0; i < query.length(); i++) {
            char ch = query.charAt(i);
            if (!Character.isWhitespace(ch)) {
                terms.add(String.valueOf(ch));
            }
        }
        for (int i = 0; i + 2 <= query.length(); i++) {
            terms.add(query.substring(i, i + 2));
        }
        return terms;
    }

    private static long scoreChunk(String text, Set<String> terms) {
        long sum = 0;
        for (String term : terms) {
            if (term.isEmpty()) {
                continue;
            }
            int cap = term.length() >= 2 ? 4 : 6;
            int n = countOccurrences(text, term);
            if (n > cap) {
                n = cap;
            }
            sum += (long) n * (long) term.length();
        }
        return sum;
    }

    private static int countOccurrences(String haystack, String needle) {
        int c = 0;
        int from = 0;
        while (from <= haystack.length() - needle.length()) {
            int i = haystack.indexOf(needle, from);
            if (i < 0) {
                break;
            }
            c++;
            from = i + Math.max(1, needle.length());
        }
        return c;
    }

    private static final class TextChunk {
        private final String sourceId;
        private final String text;

        TextChunk(String sourceId, String text) {
            this.sourceId = sourceId;
            this.text = text;
        }
    }

    private static final class ScoredChunk {
        private final TextChunk chunk;
        private final long score;

        ScoredChunk(TextChunk chunk, long score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
