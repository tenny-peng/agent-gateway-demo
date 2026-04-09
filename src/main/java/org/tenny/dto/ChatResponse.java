package org.tenny.dto;

public class ChatResponse {

    private String answer;
    private String model;
    private long latencyMs;

    public ChatResponse() {
    }

    public ChatResponse(String answer, String model, long latencyMs) {
        this.answer = answer;
        this.model = model;
        this.latencyMs = latencyMs;
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
}
