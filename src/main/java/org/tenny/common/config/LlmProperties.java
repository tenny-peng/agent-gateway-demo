package org.tenny.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private String name = "";

    private String baseUrl = "";

    private String apiKey = "";

    private String model = "";

    private int timeoutMs = 15000;

    private int streamTimeoutMs = 120000;

}
