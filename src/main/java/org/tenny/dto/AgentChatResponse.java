package org.tenny.dto;

public class AgentChatResponse {

    private String answer;
    private String model;
    private long latencyMs;
    private int stepsUsed;

    public AgentChatResponse() {
    }

    public AgentChatResponse(String answer, String model, long latencyMs, int stepsUsed) {
        this.answer = answer;
        this.model = model;
        this.latencyMs = latencyMs;
        this.stepsUsed = stepsUsed;
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

    public int getStepsUsed() {
        return stepsUsed;
    }

    public void setStepsUsed(int stepsUsed) {
        this.stepsUsed = stepsUsed;
    }
}
