package org.tenny.exception;

public class ChatLimitExceededException extends RuntimeException {

    public ChatLimitExceededException(String message) {
        super(message);
    }
}