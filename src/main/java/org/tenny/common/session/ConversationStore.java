package org.tenny.common.session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.tenny.config.AppProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ConversationStore {

    private static final String KEY_GENERIC = "agw:conv:generic:";
    private static final String KEY_LOGISTICS = "agw:conv:logistics:";

    private static final TypeReference<List<Map<String, String>>> CHAT_TYPE =
            new TypeReference<List<Map<String, String>>>() {
            };
    private static final TypeReference<List<Map<String, Object>>> AGENT_TYPE =
            new TypeReference<List<Map<String, Object>>>() {
            };

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public String newConversationId() {
        return UUID.randomUUID().toString();
    }

    public List<Map<String, String>> getChatMessages(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return null;
        }
        return getChatMessagesRedis(conversationId);
    }

    public void putChatMessages(String conversationId, List<Map<String, String>> messages) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return;
        }
        putChatMessagesRedis(conversationId, messages);
    }

    public List<Map<String, Object>> getAgentMessages(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return null;
        }
        return getAgentMessagesRedis(conversationId);
    }

    public void putAgentMessages(String conversationId, List<Map<String, Object>> messages) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return;
        }
        putAgentMessagesRedis(conversationId, messages);
    }

    private List<Map<String, String>> getChatMessagesRedis(String conversationId) {
        String json = stringRedisTemplate.opsForValue().get(KEY_GENERIC + conversationId.trim());
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            List<Map<String, String>> list = objectMapper.readValue(json, CHAT_TYPE);
            return copyStringMaps(list);
        } catch (Exception e) {
            throw new IllegalStateException("redis chat session corrupt: " + conversationId, e);
        }
    }

    private void putChatMessagesRedis(String conversationId, List<Map<String, String>> messages) {
        try {
            String json = objectMapper.writeValueAsString(copyStringMaps(messages));
            setWithOptionalTtl(KEY_GENERIC + conversationId.trim(), json);
        } catch (Exception e) {
            throw new IllegalStateException("failed to persist chat session: " + conversationId, e);
        }
    }

    private List<Map<String, Object>> getAgentMessagesRedis(String conversationId) {
        String json = stringRedisTemplate.opsForValue().get(KEY_LOGISTICS + conversationId.trim());
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, AGENT_TYPE);
            return copyObjectMaps(list);
        } catch (Exception e) {
            throw new IllegalStateException("redis agent session corrupt: " + conversationId, e);
        }
    }

    private void putAgentMessagesRedis(String conversationId, List<Map<String, Object>> messages) {
        try {
            String json = objectMapper.writeValueAsString(copyObjectMaps(messages));
            setWithOptionalTtl(KEY_LOGISTICS + conversationId.trim(), json);
        } catch (Exception e) {
            throw new IllegalStateException("failed to persist agent session: " + conversationId, e);
        }
    }

    private void setWithOptionalTtl(String key, String json) {
        int hours = appProperties.getConversation().getRedisTtlHours();
        if (hours > 0) {
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofHours(hours));
        } else {
            stringRedisTemplate.opsForValue().set(key, json);
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
