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

}
