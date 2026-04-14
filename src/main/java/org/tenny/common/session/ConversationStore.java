package org.tenny.common.session;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory multi-turn history (demo only; replace with Redis for production).
 * <p>
 * {@code chatSessions}: {@link org.tenny.generic} plain chat.
 * {@code agentSessions}: {@link org.tenny.logistics} tool-calling agent.
 * </p>
 */
@Component
public class ConversationStore {

    private final ConcurrentHashMap<String, List<Map<String, String>>> chatSessions = new ConcurrentHashMap<String, List<Map<String, String>>>();
    private final ConcurrentHashMap<String, List<Map<String, Object>>> agentSessions = new ConcurrentHashMap<String, List<Map<String, Object>>>();

    public String newConversationId() {
        return UUID.randomUUID().toString();
    }

    public List<Map<String, String>> getChatMessages(String conversationId) {
        List<Map<String, String>> list = chatSessions.get(conversationId);
        if (list == null) {
            return null;
        }
        return copyStringMaps(list);
    }

    public void putChatMessages(String conversationId, List<Map<String, String>> messages) {
        chatSessions.put(conversationId, copyStringMaps(messages));
    }

    public List<Map<String, Object>> getAgentMessages(String conversationId) {
        List<Map<String, Object>> list = agentSessions.get(conversationId);
        if (list == null) {
            return null;
        }
        return copyObjectMaps(list);
    }

    public void putAgentMessages(String conversationId, List<Map<String, Object>> messages) {
        agentSessions.put(conversationId, copyObjectMaps(messages));
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
