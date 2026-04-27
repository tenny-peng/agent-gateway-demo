package org.tenny;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan("org.tenny.common.config")
public class AgentGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentGatewayApplication.class, args);
    }
}
