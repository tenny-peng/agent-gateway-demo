package org.tenny.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    /**
     * Enable Redis cache. If false, uses in-memory cache.
     */
    private boolean enabled = true;

    /**
     * Server-side session (Redis) TTL in hours for opaque UUID tokens.
     */
    private int sessionExpireHours = 168;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSessionExpireHours() {
        return sessionExpireHours;
    }

    public void setSessionExpireHours(int sessionExpireHours) {
        this.sessionExpireHours = sessionExpireHours;
    }
}
