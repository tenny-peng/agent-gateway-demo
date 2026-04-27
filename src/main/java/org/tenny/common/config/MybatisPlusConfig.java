package org.tenny.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({"org.tenny.**.mapper"})
public class MybatisPlusConfig {
}
