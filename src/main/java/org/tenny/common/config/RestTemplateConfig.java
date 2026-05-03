package org.tenny.common.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.tenny.llmconfig.service.LlmConfigService;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate llmRestTemplate(RestTemplateBuilder builder, LlmProperties llmProperties) {
        int readMs = Math.max(llmProperties.getTimeoutMs(), llmProperties.getStreamTimeoutMs());
        return builder
                .setConnectTimeout(Duration.ofMillis(llmProperties.getTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(readMs))
                .build();
    }

    @Bean
    public RestTemplate webSearchRestTemplate(RestTemplateBuilder builder, AppProperties appProperties) {
        long ms = Math.max(1000L, appProperties.getWebSearch().getTimeoutMs());
        return builder
                .setConnectTimeout(Duration.ofMillis(ms))
                .setReadTimeout(Duration.ofMillis(ms))
                .build();
    }
}
