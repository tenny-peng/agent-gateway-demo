package org.tenny.logistics.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AgentChatResponse {

    private String answer;
    private String model;
    private long latencyMs;
    private int stepsUsed;
    private String conversationId;

    public AgentChatResponse() {
    }

    public AgentChatResponse(String answer, String model, long latencyMs, int stepsUsed, String conversationId) {
        this.answer = answer;
        this.model = model;
        this.latencyMs = latencyMs;
        this.stepsUsed = stepsUsed;
        this.conversationId = conversationId;
    }

}
