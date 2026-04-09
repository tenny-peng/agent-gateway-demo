package org.tenny.dto;

import javax.validation.constraints.NotBlank;

public class ChatRequest {

    @NotBlank
    private String message;

    /**
     * Omit or empty to start a new conversation; use the value returned by the previous response to continue.
     */
    private String conversationId;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
