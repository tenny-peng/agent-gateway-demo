package org.tenny.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({"org.tenny.auth.mapper", "org.tenny.skill.mapper", "org.tenny.config.mapper"})
public class MybatisPlusConfig {
}
