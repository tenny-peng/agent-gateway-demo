package org.tenny.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    /**
     * Server-side session (Redis) TTL in hours for opaque UUID tokens.
     */
    private int sessionExpireHours = 168;

    public int getSessionExpireHours() {
        return sessionExpireHours;
    }

    public void setSessionExpireHours(int sessionExpireHours) {
        this.sessionExpireHours = sessionExpireHours;
    }
}
