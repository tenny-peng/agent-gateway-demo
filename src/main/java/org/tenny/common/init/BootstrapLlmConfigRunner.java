package org.tenny.common.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.tenny.common.config.LlmProperties;
import org.tenny.llmconfig.entity.LlmConfig;
import org.tenny.llmconfig.service.LlmConfigService;

/**
 * Bootstrap runner to initialize default LLM configuration in database.
 * Only creates default config if no configurations exist at all.
 * If configs exist but none are active, activates the first one.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapLlmConfigRunner implements ApplicationRunner {

    private final LlmConfigService llmConfigService;
    private final LlmProperties llmProperties;

    @Override
    public void run(ApplicationArguments args) {
        String name = llmProperties.getName();
        LlmConfig llmConfig = llmConfigService.getConfigByName(name);
        if (llmConfig != null) {
            llmConfig.setBaseUrl(llmProperties.getBaseUrl());
            llmConfig.setApiKey(llmProperties.getApiKey());
            llmConfig.setModel(llmProperties.getModel());
            llmConfig.setTimeoutMs(llmProperties.getTimeoutMs());
            llmConfig.setStreamTimeoutMs(llmProperties.getStreamTimeoutMs());
            llmConfigService.updateById(llmConfig);
            return;
        }
        LlmConfig defaultConfig = new LlmConfig();
        defaultConfig.setName(name);
        defaultConfig.setBaseUrl(llmProperties.getBaseUrl());
        defaultConfig.setApiKey(llmProperties.getApiKey());
        defaultConfig.setModel(llmProperties.getModel());
        defaultConfig.setTimeoutMs(llmProperties.getTimeoutMs());
        defaultConfig.setStreamTimeoutMs(llmProperties.getStreamTimeoutMs());
        defaultConfig.setIsActive(true);
        llmConfigService.add(defaultConfig);
    }
}