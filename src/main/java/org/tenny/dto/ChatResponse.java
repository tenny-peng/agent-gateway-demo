package org.tenny.dto;

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

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
