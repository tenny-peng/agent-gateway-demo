package org.tenny.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    private Security security = new Security();
    private Conversation conversation = new Conversation();
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();
    /**
     * Generic-chat web search (Tavily). Requires {@code api-key} when users enable联网.
     */
    private WebSearch webSearch = new WebSearch();
    
    @Setter
    @Getter
    public static class Security {
        /**
         * Server-side session (Redis) TTL in hours for opaque UUID tokens.
         */
        private int sessionExpireHours = 168;
    }
    
    @Setter
    @Getter
    public static class Conversation {
        /**
         * Hours to keep cached message lists in Redis. 
         * 0 means no TTL (persist until manual eviction / Redis policy).
         */
        private int redisTtlHours = 168;
    }
    
    @Setter
    @Getter
    public static class BootstrapAdmin {
        private String username = "";
        private String password = "";
    }

    @Setter
    @Getter
    public static class WebSearch {
        /**
         * Tavily API key (e.g. env {@code TAVILY_API_KEY}).
         */
        private String apiKey = "";
        private int maxResults = 5;
        private int maxSnippetCharsPerResult = 800;
        private int timeoutMs = 15000;
    }
}