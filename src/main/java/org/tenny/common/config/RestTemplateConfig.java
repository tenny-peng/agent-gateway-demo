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
    public RestTemplate llmRestTemplate(RestTemplateBuilder builder, LlmConfigService llmConfigService) {
        org.tenny.llmconfig.entity.LlmConfig c = llmConfigService.getActiveConfig();
        int connectMs = c.getTimeoutMs() != null ? c.getTimeoutMs() : 15000;
        int streamMs = c.getStreamTimeoutMs() != null ? c.getStreamTimeoutMs() : connectMs;
        int readMs = Math.max(connectMs, streamMs);
        return builder
                .setConnectTimeout(Duration.ofMillis(connectMs))
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
