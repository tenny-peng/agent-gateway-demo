package org.tenny.client;

public final class LlmToolCall {

    private final String id;
    private final String name;
    private final String arguments;

    public LlmToolCall(String id, String name, String arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArguments() {
        return arguments;
    }
}
