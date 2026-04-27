package org.tenny.llmconfig.service;

import com.baomidou.mybatisplus.extension.service.IService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tenny.llmconfig.entity.LlmConfig;
import org.tenny.llmconfig.mapper.LlmConfigMapper;

import java.time.LocalDateTime;
import java.util.List;

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