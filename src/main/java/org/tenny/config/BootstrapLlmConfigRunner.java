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
import java.util.List;

/**
 * Bootstrap runner to initialize default LLM configuration in database.
 * Only creates default config if no configurations exist at all.
 * If configs exist but none are active, activates the first one.
 */
@Component
public class BootstrapLlmConfigRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapLlmConfigRunner.class);
    private static final String DEFAULT_CONFIG_NAME = "Default DeepSeek Configuration";

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
            log.info("No active LLM configuration found, checking for default config");
        }

        // First, check if the default config name already exists
        LlmConfig defaultConfig = llmConfigService.getConfigByName(DEFAULT_CONFIG_NAME);
        if (defaultConfig != null) {
            // Default config exists - activate it if not already active
            if (!defaultConfig.getIsActive()) {
                llmConfigService.setActiveConfig(defaultConfig.getId());
                log.info("Activated existing default LLM configuration: {}", DEFAULT_CONFIG_NAME);
            } else {
                log.info("Default LLM configuration is already active");
            }
            return;
        }

        // No default config found - check if any configs exist at all
        List<LlmConfig> allConfigs = llmConfigService.getAllConfigs();
        
        if (allConfigs.isEmpty()) {
            // No configs at all - create default
            createDefaultConfig();
        } else {
            // Configs exist but none with default name - activate the first one
            log.info("Found {} existing LLM configuration(s), activating first config: {}", 
                allConfigs.size(), allConfigs.get(0).getName());
            try {
                llmConfigService.setActiveConfig(allConfigs.get(0).getId());
                log.info("Successfully activated existing LLM configuration: {}", allConfigs.get(0).getName());
            } catch (Exception ex) {
                log.error("Failed to activate existing configuration", ex);
            }
        }
    }

    private void createDefaultConfig() {
        // Create default config from environment/application.yml
        LlmConfig defaultConfig = new LlmConfig();
        defaultConfig.setName(DEFAULT_CONFIG_NAME);
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