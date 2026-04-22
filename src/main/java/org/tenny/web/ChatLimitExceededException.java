package org.tenny.web;

public class ChatLimitExceededException extends RuntimeException {

    public ChatLimitExceededException(String message) {
        super(message);
    }
}