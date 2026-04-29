package org.tenny.common.helper.llmclient.dto;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * One non-streaming chat completion: either final text and/or tool_calls to execute.
 */
@Getter
public final class LlmCompletionResult {

    private final String content;
    /** Optional model chain-of-thought (e.g. {@code reasoning_content}); not for tool rounds. */
    private final String reasoning;
    private final List<LlmToolCall> toolCalls;
    /** Raw assistant message map for appending back to messages (preserves tool_calls shape). */
    private final java.util.Map<String, Object> assistantMessage;

    public LlmCompletionResult(String content,
                               List<LlmToolCall> toolCalls,
                               java.util.Map<String, Object> assistantMessage) {
        this(content, toolCalls, assistantMessage, null);
    }

    public LlmCompletionResult(String content,
                               List<LlmToolCall> toolCalls,
                               java.util.Map<String, Object> assistantMessage,
                               String reasoning) {
        this.content = content;
        this.reasoning = reasoning;
        this.toolCalls = toolCalls == null ? Collections.<LlmToolCall>emptyList() : toolCalls;
        this.assistantMessage = assistantMessage;
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

}
