package org.tenny;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.tenny.config.AgentProperties;
import org.tenny.config.AppSecurityProperties;
import org.tenny.config.BootstrapAdminProperties;
import org.tenny.config.ConversationRedisProperties;

import org.tenny.config.RagProperties;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties({
        AgentProperties.class,
        RagProperties.class,
        AppSecurityProperties.class,
        BootstrapAdminProperties.class,
        ConversationRedisProperties.class
})
public class AgentGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentGatewayApplication.class, args);
    }
}
