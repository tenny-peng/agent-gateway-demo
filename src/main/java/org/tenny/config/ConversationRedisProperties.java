package org.tenny.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis cache for LLM message lists (multi-instance safe). DB remains source of truth for history UI.
 */
@ConfigurationProperties(prefix = "app.conversation")
public class ConversationRedisProperties {

    /**
     * Hours to keep cached message lists in Redis. 0 means no TTL (persist until manual eviction / Redis policy).
     */
    private int redisTtlHours = 168;

    public int getRedisTtlHours() {
        return redisTtlHours;
    }

    public void setRedisTtlHours(int redisTtlHours) {
        this.redisTtlHours = redisTtlHours;
    }
}
