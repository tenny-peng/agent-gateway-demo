package org.tenny.config.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tenny.config.entity.LlmConfig;
import org.tenny.config.mapper.LlmConfigMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing LLM configurations.
 */
@Service
public class LlmConfigService {

    private static final Logger log = LoggerFactory.getLogger(LlmConfigService.class);

    private final LlmConfigMapper llmConfigMapper;

    public LlmConfigService(LlmConfigMapper llmConfigMapper) {
        this.llmConfigMapper = llmConfigMapper;
    }

    /**
     * Get the active LLM configuration.
     */
    @Cacheable(value = "llmConfig", key = "'active'")
    public LlmConfig getActiveConfig() {
        LlmConfig config = llmConfigMapper.selectActiveConfig();
        if (config == null) {
            throw new RuntimeException("No active LLM configuration found");
        }
        return config;
    }

    /**
     * Get all LLM configurations.
     */
    public List<LlmConfig> getAllConfigs() {
        return llmConfigMapper.selectList(null);
    }

    /**
     * Get a configuration by ID.
     */
    public LlmConfig getConfigById(Long id) {
        return llmConfigMapper.selectById(id);
    }

    /**
     * Get a configuration by name.
     */
    public LlmConfig getConfigByName(String name) {
        return llmConfigMapper.selectByName(name);
    }

    /**
     * Create a new LLM configuration.
     */
    @Transactional
    public LlmConfig createConfig(LlmConfig config) {
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());

        // If this is the first config or marked as active, ensure only one active config
        if (config.getIsActive() != null && config.getIsActive()) {
            deactivateAllConfigs();
        } else if (llmConfigMapper.selectCount(null) == 0) {
            // If this is the first config, make it active
            config.setIsActive(true);
        }

        llmConfigMapper.insert(config);
        log.info("Created LLM config: {}", config.getName());

        return config;
    }

    /**
     * Update an existing LLM configuration.
     */
    @Transactional
    @CacheEvict(value = "llmConfig", allEntries = true)
    public LlmConfig updateConfig(Long id, LlmConfig updatedConfig) {
        LlmConfig existing = llmConfigMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("LLM config not found: " + id);
        }

        // Update fields
        if (updatedConfig.getName() != null) {
            existing.setName(updatedConfig.getName());
        }
        if (updatedConfig.getBaseUrl() != null) {
            existing.setBaseUrl(updatedConfig.getBaseUrl());
        }
        if (updatedConfig.getApiKey() != null) {
            existing.setApiKey(updatedConfig.getApiKey());
        }
        if (updatedConfig.getModel() != null) {
            existing.setModel(updatedConfig.getModel());
        }
        if (updatedConfig.getTimeoutMs() != null) {
            existing.setTimeoutMs(updatedConfig.getTimeoutMs());
        }
        if (updatedConfig.getStreamTimeoutMs() != null) {
            existing.setStreamTimeoutMs(updatedConfig.getStreamTimeoutMs());
        }

        // Handle active status change
        if (updatedConfig.getIsActive() != null && !updatedConfig.getIsActive().equals(existing.getIsActive())) {
            if (updatedConfig.getIsActive()) {
                deactivateAllConfigs();
            }
            existing.setIsActive(updatedConfig.getIsActive());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        llmConfigMapper.updateById(existing);

        log.info("Updated LLM config: {}", existing.getName());
        return existing;
    }

    /**
     * Set a configuration as active (deactivates all others).
     */
    @Transactional
    @CacheEvict(value = "llmConfig", allEntries = true)
    public LlmConfig setActiveConfig(Long id) {
        LlmConfig config = llmConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("LLM config not found: " + id);
        }

        deactivateAllConfigs();
        config.setIsActive(true);
        config.setUpdatedAt(LocalDateTime.now());
        llmConfigMapper.updateById(config);

        log.info("Set LLM config as active: {}", config.getName());
        return config;
    }

    /**
     * Delete a configuration.
     */
    @Transactional
    @CacheEvict(value = "llmConfig", allEntries = true)
    public void deleteConfig(Long id) {
        LlmConfig config = llmConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("LLM config not found: " + id);
        }

        llmConfigMapper.deleteById(id);
        log.info("Deleted LLM config: {}", config.getName());

        // If we deleted the active config, make another one active if available
        if (config.getIsActive() != null && config.getIsActive()) {
            List<LlmConfig> remaining = llmConfigMapper.selectList(null);
            if (!remaining.isEmpty()) {
                setActiveConfig(remaining.get(0).getId());
            }
        }
    }

    /**
     * Deactivate all configurations.
     */
    private void deactivateAllConfigs() {
        List<LlmConfig> allConfigs = llmConfigMapper.selectList(null);
        for (LlmConfig config : allConfigs) {
            if (config.getIsActive() != null && config.getIsActive()) {
                config.setIsActive(false);
                config.setUpdatedAt(LocalDateTime.now());
                llmConfigMapper.updateById(config);
            }
        }
    }
}