package org.tenny.generic.service;

import org.springframework.stereotype.Service;
import org.tenny.auth.model.SessionType;
import org.tenny.auth.entity.UserConversationMessage;
import org.tenny.auth.mapper.UserConversationMessageMapper;
import org.tenny.auth.service.ConversationMessageService;
import org.tenny.auth.service.ConversationTrackingService;
import org.tenny.client.LlmClient;
import org.tenny.client.LlmStreamClient;
import org.tenny.common.session.ConversationStore;
import org.tenny.config.LlmProperties;
import org.tenny.dto.ChatResponse;
import org.tenny.rag.RagService;
import org.tenny.skill.service.SkillInjectService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic chat (no tools): multi-turn plain LLM completion + optional RAG on system.
 */
@Service
public class GenericChatService {

    private static final String CHAT_SYSTEM_BASE = "You are a helpful assistant.";

    private final LlmClient llmClient;
    private final LlmStreamClient llmStreamClient;
    private final LlmProperties llmProperties;
    private final ConversationStore conversationStore;
    private final RagService ragService;
    private final SkillInjectService skillInjectService;
    private final ConversationTrackingService conversationTrackingService;
    private final ConversationMessageService conversationMessageService;
    private final UserConversationMessageMapper userConversationMessageMapper;

    public GenericChatService(LlmClient llmClient,
                              LlmStreamClient llmStreamClient,
                              LlmProperties llmProperties,
                              ConversationStore conversationStore,
                              RagService ragService,
                              SkillInjectService skillInjectService,
                              ConversationTrackingService conversationTrackingService,
                              ConversationMessageService conversationMessageService,
                              UserConversationMessageMapper userConversationMessageMapper) {
        this.llmClient = llmClient;
        this.llmStreamClient = llmStreamClient;
        this.llmProperties = llmProperties;
        this.conversationStore = conversationStore;
        this.ragService = ragService;
        this.skillInjectService = skillInjectService;
        this.conversationTrackingService = conversationTrackingService;
        this.conversationMessageService = conversationMessageService;
        this.userConversationMessageMapper = userConversationMessageMapper;
    }

    /**
     * Plain chat (no tools). Pass {@code conversationId} from the previous {@link ChatResponse} to continue.
     */
    public ChatResponse chat(String userMessage, String conversationId, long userId) {
        String id;
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        if (conversationId == null || conversationId.trim().isEmpty()) {
            id = conversationStore.newConversationId();
            conversationTrackingService.recordIfNew(userId, id, SessionType.GENERIC);
            messages.add(chatSystem());
            messages.add(userMessage(userMessage));
        } else {
            id = conversationId.trim();
            List<Map<String, String>> previous = conversationStore.getChatMessages(id);
            if (previous == null) {
                previous = restoreChatMessages(userId, id);
            }
            messages.addAll(previous);
            messages.add(userMessage(userMessage));
        }

        ragService.augmentChatSystem(messages, userMessage);
        skillInjectService.augmentChatSystem(messages, userMessage, userId);

        long start = System.currentTimeMillis();
        String answer = llmClient.chatCompletions(messages);
        long latency = System.currentTimeMillis() - start;

        List<Map<String, String>> toSave = new ArrayList<Map<String, String>>(messages);
        toSave.add(assistantMessage(answer));
        ragService.stripRagFromChatMessages(toSave);
        conversationStore.putChatMessages(id, toSave);
        conversationMessageService.appendMessage(userId, id, SessionType.GENERIC, "user", userMessage, null, userMessage);
        conversationMessageService.appendMessage(userId, id, SessionType.GENERIC, "assistant", answer, null, userMessage);

        return new ChatResponse(answer, llmProperties.getModel(), latency, id);
    }

    /**
     * Build message list for streaming; first SSE event should expose {@link StreamChatContext#getConversationId()}.
     */
    public StreamChatContext prepareStreamContext(String userMessage, String conversationId, long userId) {
        String id;
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        if (conversationId == null || conversationId.trim().isEmpty()) {
            id = conversationStore.newConversationId();
            conversationTrackingService.recordIfNew(userId, id, SessionType.GENERIC);
            messages.add(chatSystem());
            messages.add(userMessage(userMessage));
        } else {
            id = conversationId.trim();
            List<Map<String, String>> previous = conversationStore.getChatMessages(id);
            if (previous == null) {
                previous = restoreChatMessages(userId, id);
            }
            messages.addAll(previous);
            messages.add(userMessage(userMessage));
        }
        ragService.augmentChatSystem(messages, userMessage);
        skillInjectService.augmentChatSystem(messages, userMessage, userId);
        return new StreamChatContext(id, messages, userId, userMessage);
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
        ragService.stripRagFromChatMessages(next);
        skillInjectService.stripSkillsFromChatMessages(next);
        conversationStore.putChatMessages(ctx.getConversationId(), next);
        conversationMessageService.appendMessage(
                ctx.getUserId(), ctx.getConversationId(), SessionType.GENERIC, "user", ctx.getUserMessage(), null, ctx.getUserMessage());
        conversationMessageService.appendMessage(
                ctx.getUserId(), ctx.getConversationId(), SessionType.GENERIC, "assistant", acc.toString(), null, ctx.getUserMessage());
    }

    public static final class StreamChatContext {
        private final String conversationId;
        private final List<Map<String, String>> messages;
        private final long userId;
        private final String userMessage;

        public StreamChatContext(String conversationId, List<Map<String, String>> messages, long userId, String userMessage) {
            this.conversationId = conversationId;
            this.messages = messages;
            this.userId = userId;
            this.userMessage = userMessage;
        }

        public String getConversationId() {
            return conversationId;
        }

        public List<Map<String, String>> getMessages() {
            return messages;
        }

        public long getUserId() {
            return userId;
        }

        public String getUserMessage() {
            return userMessage;
        }
    }

    private static Map<String, String> chatSystem() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("role", "system");
        m.put("content", CHAT_SYSTEM_BASE);
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

    private List<Map<String, String>> restoreChatMessages(long userId, String conversationId) {
        List<UserConversationMessage> rows = userConversationMessageMapper.selectMessages(
                userId, conversationId, SessionType.GENERIC.name(), 2000);
        if (rows == null || rows.isEmpty()) {
            throw new IllegalStateException("unknown conversationId: " + conversationId);
        }
        List<Map<String, String>> restored = new ArrayList<Map<String, String>>();
        restored.add(chatSystem());
        for (UserConversationMessage row : rows) {
            if ("user".equals(row.getRole())) {
                restored.add(userMessage(row.getContent()));
            } else if ("assistant".equals(row.getRole())) {
                restored.add(assistantMessage(row.getContent()));
            }
        }
        conversationStore.putChatMessages(conversationId, restored);
        return restored;
    }
}
