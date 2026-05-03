package org.tenny.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /** Max LLM round-trips (each may include tool execution). */
    private int maxSteps = 8;

}
