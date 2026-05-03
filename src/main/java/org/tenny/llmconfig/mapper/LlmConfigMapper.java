package org.tenny.llmconfig.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.tenny.llmconfig.entity.LlmConfig;

/**
 * Mapper for LlmConfig entity.
 */
public interface LlmConfigMapper extends BaseMapper<LlmConfig> {

    /**
     * Get the active LLM configuration.
     */
    @Select("SELECT * FROM llm_config WHERE is_active = 1 LIMIT 1")
    LlmConfig selectActiveConfig();

    /**
     * Get configuration by name.
     */
    @Select("SELECT * FROM llm_config WHERE name = #{name} LIMIT 1")
    LlmConfig selectByName(String name);
}