package org.tenny.service;

import org.springframework.stereotype.Service;
import org.tenny.client.LlmClient;
import org.tenny.config.LlmProperties;
import org.tenny.dto.ChatResponse;

@Service
public class ChatService {

    private final LlmClient llmClient;
    private final LlmProperties llmProperties;

    public ChatService(LlmClient llmClient, LlmProperties llmProperties) {
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
    }

    public ChatResponse chat(String userMessage) {
        long start = System.currentTimeMillis();
        String answer = llmClient.chatCompletions(LlmClient.defaultMessages(userMessage));
        long latency = System.currentTimeMillis() - start;
        return new ChatResponse(answer, llmProperties.getModel(), latency);
    }
}
