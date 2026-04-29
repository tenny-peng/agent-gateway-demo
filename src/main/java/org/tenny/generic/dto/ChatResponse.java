package org.tenny.generic.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatResponse {

    private String answer;
    /** Present when the model returned chain-of-thought (e.g. reasoning_content). */
    private String reasoning;
    private String model;
    private long latencyMs;
    /** Pass back on the next request to continue the same conversation (plain chat). */
    private String conversationId;

    public ChatResponse() {
    }

    public ChatResponse(String answer, String model, long latencyMs, String conversationId) {
        this.answer = answer;
        this.model = model;
        this.latencyMs = latencyMs;
        this.conversationId = conversationId;
    }

    public ChatResponse(String answer, String reasoning, String model, long latencyMs, String conversationId) {
        this.answer = answer;
        this.reasoning = reasoning;
        this.model = model;
        this.latencyMs = latencyMs;
        this.conversationId = conversationId;
    }

}
