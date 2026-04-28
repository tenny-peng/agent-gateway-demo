package org.tenny.generic.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tenny.user.entity.AppUser;
import org.tenny.conversation.entity.UserConversationMessage;
import org.tenny.user.mapper.AppUserMapper;
import org.tenny.conversation.mapper.UserConversationMessageMapper;
import org.tenny.generic.enums.SessionType;
import org.tenny.conversation.service.ConversationMessageService;
import org.tenny.conversation.service.ConversationTrackingService;
import org.tenny.common.helper.llmclient.LlmClient;
import org.tenny.common.helper.llmclient.LlmStreamClient;
import org.tenny.common.session.ConversationStore;
import org.tenny.generic.dto.ChatResponse;
import org.tenny.common.exception.ChatLimitExceededException;
import org.tenny.llmconfig.service.LlmConfigService;
import org.tenny.skill.service.SkillInjectService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic chat (no tools): multi-turn plain LLM completion + optional RAG on system.
 */
@Service
@RequiredArgsConstructor
public class GenericChatService {

    private static final String CHAT_SYSTEM_BASE = "You are a helpful assistant.";

    private final LlmClient llmClient;
    private final LlmStreamClient llmStreamClient;
    private final LlmConfigService llmConfigService;
    private final ConversationStore conversationStore;
    private final SkillInjectService skillInjectService;
    private final ConversationTrackingService conversationTrackingService;
    private final ConversationMessageService conversationMessageService;
    private final UserConversationMessageMapper userConversationMessageMapper;
    private final AppUserMapper appUserMapper;
    private final WebSearchService webSearchService;

    private void checkChatLimit(long userId) {
        AppUser user = appUserMapper.selectById(userId);
        if (user != null && Boolean.TRUE.equals(user.getChatLimitEnabled())) {
            // Check message count limit (e.g., 10 messages per user)
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserConversationMessage> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            wrapper.eq("user_id", userId);
            Long messageCount = userConversationMessageMapper.selectCount(wrapper);
            if (messageCount >= 10) { // Limit to 10 messages
                throw new ChatLimitExceededException("Chat limit exceeded (10 messages). Please contact administrator to increase limit.");
            }
        }
    }

    /**
     * Plain chat (no tools). Pass {@code conversationId} from the previous {@link ChatResponse} to continue.
     *
     * @param webSearch when {@link Boolean#TRUE}, runs web search and injects snippets for this turn only (not stored in history).
     */
    public ChatResponse chat(String userMessage, String conversationId, long userId, Boolean webSearch) {
        checkChatLimit(userId);
        String id;
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        if (conversationId == null || conversationId.trim().isEmpty()) {
            id = conversationStore.newConversationId();
            conversationTrackingService.recordIfNew(userId, id, SessionType.GENERIC);
            messages.add(chatSystem());
            messages.add(userMessage(userMessage));
        } else {
            id = conversationId.trim();
            List<Map<String, String>> previous = conversationStore.getChatMessages(id);
            if (previous == null) {
                previous = restoreChatMessages(userId, id);
            }
            messages.addAll(previous);
            messages.add(userMessage(userMessage));
        }

        skillInjectService.augmentChatSystem(messages, userMessage, userId);

        List<Map<String, String>> forLlm = new ArrayList<Map<String, String>>(messages);
        injectWebSearchContext(forLlm, userMessage, Boolean.TRUE.equals(webSearch));

        long start = System.currentTimeMillis();
        String answer = llmClient.chatCompletions(forLlm);
        long latency = System.currentTimeMillis() - start;

        List<Map<String, String>> toSave = new ArrayList<Map<String, String>>(messages);
        toSave.add(assistantMessage(answer));
        conversationStore.putChatMessages(id, toSave);
        conversationMessageService.appendMessage(userId, id, SessionType.GENERIC, "user", userMessage, null, userMessage);
        conversationMessageService.appendMessage(userId, id, SessionType.GENERIC, "assistant", answer, null, userMessage);

        return new ChatResponse(answer, llmConfigService.getActiveConfig().getModel(), latency, id);
    }

    /**
     * Build message list for streaming; first SSE event should expose {@link StreamChatContext#getConversationId()}.
     */
    public StreamChatContext prepareStreamContext(String userMessage, String conversationId, long userId,
                                                  Boolean webSearch) {
        checkChatLimit(userId);
        String id;
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        if (conversationId == null || conversationId.trim().isEmpty()) {
            id = conversationStore.newConversationId();
            conversationTrackingService.recordIfNew(userId, id, SessionType.GENERIC);
            messages.add(chatSystem());
            messages.add(userMessage(userMessage));
        } else {
            id = conversationId.trim();
            List<Map<String, String>> previous = conversationStore.getChatMessages(id);
            if (previous == null) {
                previous = restoreChatMessages(userId, id);
            }
            messages.addAll(previous);
            messages.add(userMessage(userMessage));
        }
        skillInjectService.augmentChatSystem(messages, userMessage, userId);
        return new StreamChatContext(id, messages, userId, userMessage, Boolean.TRUE.equals(webSearch));
    }

    /**
     * Stream tokens then append assistant text to the session.
     */
    public void streamWithContext(StreamChatContext ctx, LlmStreamClient.StreamDeltaConsumer onDelta) throws IOException {
        List<Map<String, String>> forLlm = new ArrayList<Map<String, String>>(ctx.getMessages());
        injectWebSearchContext(forLlm, ctx.getUserMessage(), ctx.isWebSearch());
        StringBuilder acc = new StringBuilder();
        llmStreamClient.streamChatCompletions(forLlm, piece -> {
            acc.append(piece);
            onDelta.onDelta(piece);
        });
        List<Map<String, String>> next = new ArrayList<Map<String, String>>(ctx.getMessages());
        next.add(assistantMessage(acc.toString()));
        skillInjectService.stripSkillsFromChatMessages(next);
        conversationStore.putChatMessages(ctx.getConversationId(), next);
        conversationMessageService.appendMessage(
                ctx.getUserId(), ctx.getConversationId(), SessionType.GENERIC, "user", ctx.getUserMessage(), null, ctx.getUserMessage());
        conversationMessageService.appendMessage(
                ctx.getUserId(), ctx.getConversationId(), SessionType.GENERIC, "assistant", acc.toString(), null, ctx.getUserMessage());
    }

    @Getter
    public static final class StreamChatContext {
        private final String conversationId;
        private final List<Map<String, String>> messages;
        private final long userId;
        private final String userMessage;
        private final boolean webSearch;

        public StreamChatContext(String conversationId, List<Map<String, String>> messages, long userId,
                                 String userMessage, boolean webSearch) {
            this.conversationId = conversationId;
            this.messages = messages;
            this.userId = userId;
            this.userMessage = userMessage;
            this.webSearch = webSearch;
        }

    }

    /**
     * Inserts an extra {@code system} context with web snippets immediately before the last message (current user turn).
     * Mutates {@code forLlm} only; session {@code messages} stay without this block for persistence.
     */
    private void injectWebSearchContext(List<Map<String, String>> forLlm, String latestUserText, boolean enabled) {
        if (!enabled) {
            return;
        }
        if (forLlm == null || forLlm.isEmpty()) {
            return;
        }
        WebSearchService.InjectBlock inject = webSearchService.searchAndFormatBlock(latestUserText);
        String block = inject.getBody();
        int n = inject.getResultCount();
        Map<String, String> row = new HashMap<String, String>();
        String point1 =
                n > 0
                        ? "1) 开头若写「综合了 N 条网页信息」等，其中的 N 必须等于 "
                        + n
                        + "，且必须与文末「---参考来源---」下列出的 URL 行数完全一致（恰好 "
                        + n
                        + " 行，不可多也不可少）。禁止正文写「综合了 "
                        + n
                        + " 条」而参考区只列更少。每一行对应上方摘要中的一条，该行 URL 必须与该条摘要里的 URL 完全一致（可从摘要复制）。"
                        + "若若干条与问题弱相关，在正文说明筛选结论，但参考区仍须把这 "
                        + n
                        + " 条全部列出，简述里可标注「与主题弱相关」等。\n"
                        : "1) 若上方显示未命中网页摘要：开头如实说明，不要虚构条数，不要输出「---参考来源---」参考区块。\n";
        String point4 =
                n > 0
                        ? "4) 参考链接集中收纳：正文结束后空一行输出参考区块；禁止 HTML。"
                        + "第一行且仅为 ---参考来源--- ；自第二行起恰好 "
                        + n
                        + " 行，每行纯文本：完整 http(s) URL + 半角空格 + 一句话简述（不要用 Markdown 链接）。"
                        + "这 "
                        + n
                        + " 行与上方 "
                        + n
                        + " 条摘要一一对应，顺序建议与摘要编号一致。\n"
                        : "4) 无命中时不要输出「---参考来源---」参考区块。\n";
        row.put("role", "system");
        row.put(
                "content",
                "以下是联网检索得到的候选网页摘要（本回合共 " + n + " 条命中），仅供参考，不代表全部事实。\n"
                        + "回答要求：\n"
                        + point1
                        + "2) 先归纳后作答，不要大段照抄摘要原文；遇到列举、推荐、对比、有哪些类问题须分点写多项，"
                        + "综合多条摘要里的不同要点，禁止把多项压成单条笼统结论。\n"
                        + "3) 正文排版：主体回答里不要出现 http/https 裸链，也不要使用 Markdown 链接 [文字](url)、角标式来源或「见：URL」；"
                        + "只用通顺文字陈述，避免每条内容后紧跟一个链接造成版面杂乱。\n"
                        + point4
                        + "5) 若问题缺少关键条件（如城市/地区/时间）或检索结果相互矛盾，先说明并向用户追问。\n"
                        + "6) 无法确认时要明确说不确定。\n\n"
                        + "【联网检索摘要开始】\n"
                        + block
                        + "\n【联网检索摘要结束】");
        forLlm.add(forLlm.size() - 1, row);
    }

    private static Map<String, String> chatSystem() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("role", "system");
        m.put("content", CHAT_SYSTEM_BASE);
        return m;
    }

    private static Map<String, String> userMessage(String text) {
        Map<String, String> m = new HashMap<String, String>();
        m.put("role", "user");
        m.put("content", text);
        return m;
    }

    private static Map<String, String> assistantMessage(String text) {
        Map<String, String> m = new HashMap<String, String>();
        m.put("role", "assistant");
        m.put("content", text);
        return m;
    }

    private List<Map<String, String>> restoreChatMessages(long userId, String conversationId) {
        List<UserConversationMessage> rows = userConversationMessageMapper.selectMessages(
                userId, conversationId, SessionType.GENERIC.name(), 2000);
        if (rows == null || rows.isEmpty()) {
            throw new IllegalStateException("unknown conversationId: " + conversationId);
        }
        List<Map<String, String>> restored = new ArrayList<Map<String, String>>();
        restored.add(chatSystem());
        for (UserConversationMessage row : rows) {
            if ("user".equals(row.getRole())) {
                restored.add(userMessage(row.getContent()));
            } else if ("assistant".equals(row.getRole())) {
                restored.add(assistantMessage(row.getContent()));
            }
        }
        conversationStore.putChatMessages(conversationId, restored);
        return restored;
    }
}
