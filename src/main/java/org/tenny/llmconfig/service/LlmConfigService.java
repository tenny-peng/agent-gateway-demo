package org.tenny.llmconfig.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;
import org.tenny.llmconfig.entity.LlmConfig;

/**
 * Service for managing LLM configurations.
 */
@Service
public interface LlmConfigService extends IService<LlmConfig> {

    LlmConfig getConfigByName(String name);

    LlmConfig add(LlmConfig defaultConfig);

    LlmConfig getConfigById(Long id);

    LlmConfig getActiveConfig();

    LlmConfig updateConfig(Long id, LlmConfig config);

    LlmConfig setActiveConfig(Long id);

    void deleteConfig(Long id);
}