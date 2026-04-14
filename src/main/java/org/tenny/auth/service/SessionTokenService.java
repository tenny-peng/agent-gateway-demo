package org.tenny.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.config.AppSecurityProperties;

import java.time.Duration;
import java.util.UUID;

/**
 * Opaque session tokens (UUID) stored in Redis; any app node can validate/delete by key.
 */
@Service
public class SessionTokenService {

    private static final String KEY_PREFIX = "agw:auth:session:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AppSecurityProperties appSecurityProperties;

    public SessionTokenService(StringRedisTemplate stringRedisTemplate,
                               ObjectMapper objectMapper,
                               AppSecurityProperties appSecurityProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.appSecurityProperties = appSecurityProperties;
    }

    public String createSession(long userId, String username, String role) {
        String token = UUID.randomUUID().toString();
        SessionPayload payload = new SessionPayload(userId, username, role);
        try {
            String json = objectMapper.writeValueAsString(payload);
            Duration ttl = Duration.ofHours(Math.max(1, appSecurityProperties.getSessionExpireHours()));
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + token, json, ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("session serialize failed", e);
        }
        return token;
    }

    public AuthPrincipal parseRequired(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("missing Authorization");
        }
        String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + token.trim());
        if (json == null || json.isEmpty()) {
            throw new IllegalArgumentException("invalid or expired session");
        }
        try {
            SessionPayload payload = objectMapper.readValue(json, SessionPayload.class);
            return new AuthPrincipal(payload.getUserId(), payload.getUsername(), payload.getRole());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid session payload");
        }
    }

    /**
     * Deletes the token if present (logout). Idempotent.
     */
    public void revokeBearer(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token != null && !token.trim().isEmpty()) {
            stringRedisTemplate.delete(KEY_PREFIX + token.trim());
        }
    }

    private static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
            return null;
        }
        String value = authorizationHeader.trim();
        if (value.length() < 7 || !value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return value.substring(7).trim();
    }

    private static final class SessionPayload {
        private long userId;
        private String username;
        private String role;

        SessionPayload() {
        }

        SessionPayload(long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
