package org.tenny.common.session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.tenny.config.ConversationRedisProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Multi-turn message lists shared across app nodes via Redis.
 * <p>
 * {@code generic}: plain chat maps {@code Map<String,String>}.<br>
 * {@code logistics}: agent/tool maps {@code Map<String,Object>}.
 * </p>
 * If a key is missing (expired or cold start), services may rebuild from DB and call {@code put*} again.
 */
@Component
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
    private final ConversationRedisProperties conversationRedisProperties;

    public ConversationStore(StringRedisTemplate stringRedisTemplate,
                             ObjectMapper objectMapper,
                             ConversationRedisProperties conversationRedisProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.conversationRedisProperties = conversationRedisProperties;
    }

    public String newConversationId() {
        return UUID.randomUUID().toString();
    }

    public List<Map<String, String>> getChatMessages(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return null;
        }
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

    public void putChatMessages(String conversationId, List<Map<String, String>> messages) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(copyStringMaps(messages));
            setWithOptionalTtl(KEY_GENERIC + conversationId.trim(), json);
        } catch (Exception e) {
            throw new IllegalStateException("failed to persist chat session: " + conversationId, e);
        }
    }

    public List<Map<String, Object>> getAgentMessages(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return null;
        }
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

    public void putAgentMessages(String conversationId, List<Map<String, Object>> messages) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(copyObjectMaps(messages));
            setWithOptionalTtl(KEY_LOGISTICS + conversationId.trim(), json);
        } catch (Exception e) {
            throw new IllegalStateException("failed to persist agent session: " + conversationId, e);
        }
    }

    private void setWithOptionalTtl(String key, String json) {
        int hours = conversationRedisProperties.getRedisTtlHours();
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
