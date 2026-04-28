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
     * When true, generic chat runs a web search first and injects snippets into the LLM prompt (requires Tavily API key).
     */
    private Boolean webSearch;

}
