package org.tenny.common.session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalConversationStore {

    private static final TypeReference<List<Map<String, String>>> CHAT_TYPE =
            new TypeReference<List<Map<String, String>>>() {
            };
    private static final TypeReference<List<Map<String, Object>>> AGENT_TYPE =
            new TypeReference<List<Map<String, Object>>>() {
            };

    private final ConcurrentHashMap<String, String> chatStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> agentStore = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public LocalConversationStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String newConversationId() {
        return UUID.randomUUID().toString();
    }

    public List<Map<String, String>> getChatMessages(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return null;
        }
        String json = chatStore.get(conversationId.trim());
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            List<Map<String, String>> list = objectMapper.readValue(json, CHAT_TYPE);
            return copyStringMaps(list);
        } catch (Exception e) {
            throw new IllegalStateException("chat session corrupt: " + conversationId, e);
        }
    }

    public void putChatMessages(String conversationId, List<Map<String, String>> messages) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(copyStringMaps(messages));
            chatStore.put(conversationId.trim(), json);
        } catch (Exception e) {
            throw new IllegalStateException("failed to persist chat session: " + conversationId, e);
        }
    }

    public List<Map<String, Object>> getAgentMessages(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return null;
        }
        String json = agentStore.get(conversationId.trim());
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, AGENT_TYPE);
            return copyObjectMaps(list);
        } catch (Exception e) {
            throw new IllegalStateException("agent session corrupt: " + conversationId, e);
        }
    }

    public void putAgentMessages(String conversationId, List<Map<String, Object>> messages) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(copyObjectMaps(messages));
            agentStore.put(conversationId.trim(), json);
        } catch (Exception e) {
            throw new IllegalStateException("failed to persist agent session: " + conversationId, e);
        }
    }

    private static List<Map<String, String>> copyStringMaps(List<Map<String, String>> list) {
        List<Map<String, String>> out = new ArrayList<Map<String, String>>();
        for (Map<String, String> m : list) {
            out.add(new HashMap<String, String>(m));
        }
        return out;
    }

    private static List<Map<String, Object>> copyObjectMaps(List<Map<String, Object>> list) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> m : list) {
            out.add(new HashMap<String, Object>(m));
        }
        return out;
    }
}