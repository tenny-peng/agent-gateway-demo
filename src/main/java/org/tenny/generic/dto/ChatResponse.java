package org.tenny.generic.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatResponse {

    private String answer;
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

}
