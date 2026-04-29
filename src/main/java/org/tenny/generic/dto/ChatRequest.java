package org.tenny.generic.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Setter
@Getter
public class ChatRequest {

    @NotBlank
    private String message;

    /**
     * Omit or empty to start a new conversation; use the value returned by the previous response to continue.
     */
    private String conversationId;

    /**
     * When true, exposes a {@code web_search} tool to the model so it may retrieve web snippets on demand (requires
     * Tavily API key). The model decides whether to call it; not every user message triggers a search.
     */
    private Boolean webSearch;

    /**
     * When true, stream and persist model chain-of-thought ({@code reasoning_content}) when the upstream provides it.
     * {@code null} or false skips reasoning in SSE, Redis, and DB (does not guarantee fewer billed tokens upstream).
     */
    private Boolean deepThinking;

}
