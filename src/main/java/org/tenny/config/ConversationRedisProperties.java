package org.tenny.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis cache for LLM message lists (multi-instance safe). DB remains source of truth for history UI.
 */
@ConfigurationProperties(prefix = "app.conversation")
public class ConversationRedisProperties {

    /**
     * Enable Redis cache. If false, uses in-memory cache.
     */
    private boolean enabled = true;

    /**
     * Hours to keep cached message lists in Redis. 0 means no TTL (persist until manual eviction / Redis policy).
     */
    private int redisTtlHours = 168;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRedisTtlHours() {
        return redisTtlHours;
    }

    public void setRedisTtlHours(int redisTtlHours) {
        this.redisTtlHours = redisTtlHours;
    }
}
