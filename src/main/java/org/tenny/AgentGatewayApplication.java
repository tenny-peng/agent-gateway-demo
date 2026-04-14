package org.tenny;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.tenny.config.AgentProperties;
import org.tenny.config.AppSecurityProperties;
import org.tenny.config.BootstrapAdminProperties;
import org.tenny.config.LlmProperties;
import org.tenny.config.RagProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        LlmProperties.class,
        AgentProperties.class,
        RagProperties.class,
        AppSecurityProperties.class,
        BootstrapAdminProperties.class
})
public class AgentGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentGatewayApplication.class, args);
    }
}
