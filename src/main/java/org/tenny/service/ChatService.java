package org.tenny.service;

import org.springframework.stereotype.Service;
import org.tenny.client.LlmClient;
import org.tenny.client.LlmStreamClient;
import org.tenny.config.LlmProperties;
import org.tenny.dto.ChatResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final LlmClient llmClient;
    private final LlmStreamClient llmStreamClient;
    private final LlmProperties llmProperties;
    private final ConversationStore conversationStore;

    public ChatService(LlmClient llmClient,
                       LlmStreamClient llmStreamClient,
                       LlmProperties llmProperties,
                       ConversationStore conversationStore) {
        this.llmClient = llmClient;
        this.llmStreamClient = llmStreamClient;
        this.llmProperties = llmProperties;
        this.conversationStore = conversationStore;
    }

    /**
     * Plain chat (no tools). Pass {@code conversationId} from the previous {@link ChatResponse} to continue.
     */
    public ChatResponse chat(String userMessage, String conversationId) {
        String id;
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        if (conversationId == null || conversationId.trim().isEmpty()) {
            id = conversationStore.newConversationId();
            messages.add(chatSystem());
            messages.add(userMessage(userMessage));
        } else {
            id = conversationId.trim();
            List<Map<String, String>> previous = conversationStore.getChatMessages(id);
            if (previous == null) {
                throw new IllegalStateException("unknown conversationId: " + id);
            }
            messages.addAll(previous);
            messages.add(userMessage(userMessage));
        }

        long start = System.currentTimeMillis();
        String answer = llmClient.chatCompletions(messages);
        long latency = System.currentTimeMillis() - start;

        List<Map<String, String>> toSave = new ArrayList<Map<String, String>>(messages);
        toSave.add(assistantMessage(answer));
        conversationStore.putChatMessages(id, toSave);

        return new ChatResponse(answer, llmProperties.getModel(), latency, id);
    }

    /**
     * Build message list for streaming; first SSE event should expose {@link StreamChatContext#getConversationId()}.
     */
    public StreamChatContext prepareStreamContext(String userMessage, String conversationId) {
        String id;
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        if (conversationId == null || conversationId.trim().isEmpty()) {
            id = conversationStore.newConversationId();
            messages.add(chatSystem());
            messages.add(userMessage(userMessage));
        } else {
            id = conversationId.trim();
            List<Map<String, String>> previous = conversationStore.getChatMessages(id);
            if (previous == null) {
                throw new IllegalStateException("unknown conversationId: " + id);
            }
            messages.addAll(previous);
            messages.add(userMessage(userMessage));
        }
        return new StreamChatContext(id, messages);
    }

    /**
     * Stream tokens then append assistant text to the session.
     */
    public void streamWithContext(StreamChatContext ctx, LlmStreamClient.StreamDeltaConsumer onDelta) throws IOException {
        StringBuilder acc = new StringBuilder();
        llmStreamClient.streamChatCompletions(ctx.getMessages(), piece -> {
            acc.append(piece);
            onDelta.onDelta(piece);
        });
        List<Map<String, String>> next = new ArrayList<Map<String, String>>(ctx.getMessages());
        next.add(assistantMessage(acc.toString()));
        conversationStore.putChatMessages(ctx.getConversationId(), next);
    }

    public static final class StreamChatContext {
        private final String conversationId;
        private final List<Map<String, String>> messages;

        public StreamChatContext(String conversationId, List<Map<String, String>> messages) {
            this.conversationId = conversationId;
            this.messages = messages;
        }

        public String getConversationId() {
            return conversationId;
        }

        public List<Map<String, String>> getMessages() {
            return messages;
        }
    }

    private static Map<String, String> chatSystem() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("role", "system");
        m.put("content", "You are a helpful assistant.");
        return m;
    }

    private static Map<String, String> userMessage(String text) {
        Map<String, String> m = new HashMap<String, String>();
        m.put("role", "user");
        m.put("content", text);
        return m;
    }

    private static Map<String, String> assistantMessage(String text) {
        Map<String, String> m = new HashMap<String, String>();
        m.put("role", "assistant");
        m.put("content", text);
        return m;
    }
}
