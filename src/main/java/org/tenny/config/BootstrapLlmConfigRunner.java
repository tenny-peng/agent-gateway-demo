package org.tenny.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.tenny.config.entity.LlmConfig;
import org.tenny.config.service.LlmConfigService;

import java.time.LocalDateTime;

/**
 * Bootstrap runner to initialize default LLM configuration in database.
 * Only runs if no active LLM configuration exists.
 */
@Component
public class BootstrapLlmConfigRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapLlmConfigRunner.class);

    private final LlmConfigService llmConfigService;
    private final Environment environment;

    public BootstrapLlmConfigRunner(LlmConfigService llmConfigService, Environment environment) {
        this.llmConfigService = llmConfigService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Check if we already have an active configuration
            llmConfigService.getActiveConfig();
            log.info("Active LLM configuration already exists, skipping bootstrap");
            return;
        } catch (RuntimeException e) {
            // No active config found, proceed with bootstrap
            log.info("No active LLM configuration found, initializing default config");
        }

        // Create default config from environment/application.yml
        LlmConfig defaultConfig = new LlmConfig();
        defaultConfig.setName("Default DeepSeek Configuration");
        defaultConfig.setBaseUrl(environment.getProperty("llm.base-url", "https://ark.cn-beijing.volces.com/api/v3"));
        defaultConfig.setApiKey(environment.getProperty("llm.api-key", ""));
        defaultConfig.setModel(environment.getProperty("llm.model", "deepseek-v3-2-251201"));
        defaultConfig.setTimeoutMs(Integer.valueOf(environment.getProperty("llm.timeout-ms", "15000")));
        defaultConfig.setStreamTimeoutMs(Integer.valueOf(environment.getProperty("llm.stream-timeout-ms", "120000")));
        defaultConfig.setIsActive(true);
        defaultConfig.setCreatedAt(LocalDateTime.now());
        defaultConfig.setUpdatedAt(LocalDateTime.now());

        try {
            llmConfigService.createConfig(defaultConfig);
            log.info("Successfully initialized default LLM configuration");
        } catch (Exception e) {
            log.error("Failed to initialize default LLM configuration", e);
            throw new RuntimeException("LLM configuration bootstrap failed", e);
        }
    }
}