package org.tenny.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private boolean enabled = true;

    /** How many chunks to inject into the system prompt. */
    private int topK = 3;

    /** Split oversized paragraphs to at most this many characters. */
    private int maxChunkChars = 600;

    /**
     * Spring resource pattern, e.g. {@code classpath:rag/*.md}.
     */
    private String corpusPattern = "classpath:rag/*.md";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public int getMaxChunkChars() {
        return maxChunkChars;
    }

    public void setMaxChunkChars(int maxChunkChars) {
        this.maxChunkChars = maxChunkChars;
    }

    public String getCorpusPattern() {
        return corpusPattern;
    }

    public void setCorpusPattern(String corpusPattern) {
        this.corpusPattern = corpusPattern;
    }
}
