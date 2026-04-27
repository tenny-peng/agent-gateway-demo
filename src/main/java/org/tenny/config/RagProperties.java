package org.tenny.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
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

}
