package org.tenny.service;

import org.springframework.stereotype.Service;
import org.tenny.client.LlmClient;
import org.tenny.client.LlmStreamClient;
import org.tenny.config.LlmProperties;
import org.tenny.dto.ChatResponse;

import java.io.IOException;

@Service
public class ChatService {

    private final LlmClient llmClient;
    private final LlmStreamClient llmStreamClient;
    private final LlmProperties llmProperties;

    public ChatService(LlmClient llmClient, LlmStreamClient llmStreamClient, LlmProperties llmProperties) {
        this.llmClient = llmClient;
        this.llmStreamClient = llmStreamClient;
        this.llmProperties = llmProperties;
    }

    public ChatResponse chat(String userMessage) {
        long start = System.currentTimeMillis();
        String answer = llmClient.chatCompletions(LlmClient.defaultMessages(userMessage));
        long latency = System.currentTimeMillis() - start;
        return new ChatResponse(answer, llmProperties.getModel(), latency);
    }

    /**
     * Stream plain chat (no tools). Invokes consumer for each non-empty text delta from the model.
     */
    public void streamChat(String userMessage, LlmStreamClient.StreamDeltaConsumer onDelta) throws IOException {
        llmStreamClient.streamChatCompletions(LlmClient.defaultMessages(userMessage), onDelta);
    }
}
