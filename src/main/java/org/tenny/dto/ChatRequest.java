package org.tenny.dto;

import javax.validation.constraints.NotBlank;

public class ChatRequest {

    @NotBlank
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
