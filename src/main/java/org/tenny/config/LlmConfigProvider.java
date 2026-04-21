package org.tenny.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tenny.config.entity.LlmConfig;
import org.tenny.config.service.LlmConfigService;

/**
 * Provider for LLM configuration that loads from database instead of properties.
 * This replaces the @ConfigurationProperties approach with database-driven configuration.
 */
@Component
public class LlmConfigProvider {

    private static final Logger log = LoggerFactory.getLogger(LlmConfigProvider.class);

    private final LlmConfigService llmConfigService;

    public LlmConfigProvider(LlmConfigService llmConfigService) {
        this.llmConfigService = llmConfigService;
    }

    /**
     * Get the base URL for LLM API calls.
     */
    public String getBaseUrl() {
        try {
            LlmConfig config = llmConfigService.getActiveConfig();
            return config.getBaseUrl();
        } catch (Exception e) {
            log.error("Failed to load active LLM config, using fallback", e);
            // Fallback to default if database is not available
            return "https://ark.cn-beijing.volces.com/api/v3";
        }
    }

    /**
     * Get the API key for LLM API calls.
     */
    public String getApiKey() {
        try {
            LlmConfig config = llmConfigService.getActiveConfig();
            return config.getApiKey();
        } catch (Exception e) {
            log.error("Failed to load active LLM config, using fallback", e);
            // Fallback - in production this should probably throw an exception
            return System.getenv("API_KEY");
        }
    }

    /**
     * Get the model name.
     */
    public String getModel() {
        try {
            LlmConfig config = llmConfigService.getActiveConfig();
            return config.getModel();
        } catch (Exception e) {
            log.error("Failed to load active LLM config, using fallback", e);
            return "deepseek-v3-2-251201";
        }
    }

    /**
     * Get the timeout in milliseconds.
     */
    public int getTimeoutMs() {
        try {
            LlmConfig config = llmConfigService.getActiveConfig();
            return config.getTimeoutMs() != null ? config.getTimeoutMs() : 15000;
        } catch (Exception e) {
            log.error("Failed to load active LLM config, using fallback", e);
            return 15000;
        }
    }

    /**
     * Get the stream timeout in milliseconds.
     */
    public int getStreamTimeoutMs() {
        try {
            LlmConfig config = llmConfigService.getActiveConfig();
            return config.getStreamTimeoutMs() != null ? config.getStreamTimeoutMs() : 120000;
        } catch (Exception e) {
            log.error("Failed to load active LLM config, using fallback", e);
            return 120000;
        }
    }

    /**
     * Get the current active configuration.
     */
    public LlmConfig getActiveConfig() {
        return llmConfigService.getActiveConfig();
    }
}