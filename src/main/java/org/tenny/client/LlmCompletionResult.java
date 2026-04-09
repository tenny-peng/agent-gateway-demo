package org.tenny.client;

import java.util.Collections;
import java.util.List;

/**
 * One non-streaming chat completion: either final text and/or tool_calls to execute.
 */
public final class LlmCompletionResult {

    private final String content;
    private final List<LlmToolCall> toolCalls;
    /** Raw assistant message map for appending back to messages (preserves tool_calls shape). */
    private final java.util.Map<String, Object> assistantMessage;

    public LlmCompletionResult(String content,
                               List<LlmToolCall> toolCalls,
                               java.util.Map<String, Object> assistantMessage) {
        this.content = content;
        this.toolCalls = toolCalls == null ? Collections.<LlmToolCall>emptyList() : toolCalls;
        this.assistantMessage = assistantMessage;
    }

    public String getContent() {
        return content;
    }

    public List<LlmToolCall> getToolCalls() {
        return toolCalls;
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    public java.util.Map<String, Object> getAssistantMessage() {
        return assistantMessage;
    }
}
