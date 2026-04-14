package org.tenny.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("org.tenny.auth.mapper")
public class MybatisPlusConfig {
}
