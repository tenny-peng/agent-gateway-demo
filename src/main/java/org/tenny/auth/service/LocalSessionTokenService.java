package org.tenny.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.config.AppSecurityProperties;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Service
public class LocalSessionTokenService {

    private final ConcurrentHashMap<String, SessionEntry> sessionStore = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService cleanupScheduler;
    private final long ttlMillis;

    public LocalSessionTokenService(ObjectMapper objectMapper, AppSecurityProperties appSecurityProperties) {
        this.objectMapper = objectMapper;
        this.ttlMillis = Duration.ofHours(Math.max(1, appSecurityProperties.getSessionExpireHours())).toMillis();
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "local-session-cleanup");
            t.setDaemon(true);
            return t;
        });
        this.cleanupScheduler.scheduleAtFixedRate(this::cleanupExpired, 1, 1, TimeUnit.HOURS);
    }

    public String createSession(long userId, String username, String role) {
        String token = UUID.randomUUID().toString();
        SessionPayload payload = new SessionPayload(userId, username, role);
        try {
            String json = objectMapper.writeValueAsString(payload);
            sessionStore.put(token, new SessionEntry(json, System.currentTimeMillis() + ttlMillis));
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
        SessionEntry entry = sessionStore.get(token.trim());
        if (entry == null) {
            throw new IllegalArgumentException("invalid or expired session");
        }
        if (entry.expiredAt < System.currentTimeMillis()) {
            sessionStore.remove(token.trim());
            throw new IllegalArgumentException("invalid or expired session");
        }
        try {
            SessionPayload payload = objectMapper.readValue(entry.json, SessionPayload.class);
            return new AuthPrincipal(payload.getUserId(), payload.getUsername(), payload.getRole());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid session payload");
        }
    }

    public void revokeBearer(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token != null && !token.trim().isEmpty()) {
            sessionStore.remove(token.trim());
        }
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        sessionStore.entrySet().removeIf(entry -> entry.getValue().expiredAt < now);
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

    private static final class SessionEntry {
        final String json;
        final long expiredAt;

        SessionEntry(String json, long expiredAt) {
            this.json = json;
            this.expiredAt = expiredAt;
        }
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