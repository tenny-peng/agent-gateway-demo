package org.tenny.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate llmRestTemplate(RestTemplateBuilder builder, LlmProperties llmProperties) {
        return builder
                .setConnectTimeout(java.time.Duration.ofMillis(llmProperties.getTimeoutMs()))
                .setReadTimeout(java.time.Duration.ofMillis(llmProperties.getTimeoutMs()))
                .build();
    }
}
