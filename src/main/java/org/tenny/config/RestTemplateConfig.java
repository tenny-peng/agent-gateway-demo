package org.tenny.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.tenny.llmconfig.service.LlmConfigService;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate llmRestTemplate(RestTemplateBuilder builder, LlmConfigService llmConfigService) {
        return builder
                .setConnectTimeout(java.time.Duration.ofMillis(llmConfigService.getActiveConfig().getTimeoutMs()))
                .setReadTimeout(java.time.Duration.ofMillis(llmConfigService.getActiveConfig().getTimeoutMs()))
                .build();
    }
}
