package org.tenny.llmconfig.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tenny.llmconfig.entity.LlmConfig;
import org.tenny.llmconfig.mapper.LlmConfigMapper;
import org.tenny.llmconfig.service.LlmConfigService;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class LlmConfigServiceImpl extends ServiceImpl<LlmConfigMapper, LlmConfig> implements LlmConfigService {

    /**
     * Present entry means cache is loaded (value may be null if no row has is_active=true).
     * Null reference on the AtomicReference means invalidated / not yet loaded.
     */
    private static final class ActiveCacheEntry {
        private final LlmConfig config;

        private ActiveCacheEntry(LlmConfig config) {
            this.config = config;
        }
    }

    private final AtomicReference<ActiveCacheEntry> activeConfigCache = new AtomicReference<>();

    private void invalidateActiveConfig() {
        activeConfigCache.set(null);
    }

    @Override
    public LlmConfig getConfigByName(String name) {
        return getOne(new LambdaQueryWrapper<LlmConfig>().eq(LlmConfig::getName, name));
    }

    @Override
    public LlmConfig add(LlmConfig llmConfig) {
        if(llmConfig.getIsActive()){
            new LambdaUpdateWrapper<LlmConfig>().set(LlmConfig::getIsActive, false);
        }
        llmConfig.setCreatedAt(LocalDateTime.now());
        this.save(llmConfig);
        invalidateActiveConfig();
        return llmConfig;
    }

    @Override
    public LlmConfig getConfigById(Long id) {
        return getById(id);
    }

    @Override
    public LlmConfig getActiveConfig() {
        ActiveCacheEntry entry = activeConfigCache.get();
        if (entry != null) {
            return entry.config;
        }
        synchronized (this) {
            entry = activeConfigCache.get();
            if (entry != null) {
                return entry.config;
            }
            LlmConfig loaded = getBaseMapper().selectActiveConfig();
            activeConfigCache.set(new ActiveCacheEntry(loaded));
            return loaded;
        }
    }

    @Override
    public LlmConfig updateConfig(Long id, LlmConfig config) {
        config.setId(id);
        updateById(config);
        invalidateActiveConfig();
        return getById(id);
    }

    @Override
    @Transactional
    public LlmConfig setActiveConfig(Long id) {
        LambdaUpdateWrapper<LlmConfig> otherWrapper = new LambdaUpdateWrapper<>();
        otherWrapper.ne(LlmConfig::getId, id)
                .set(LlmConfig::getIsActive, false);
        update(otherWrapper);
        LambdaUpdateWrapper<LlmConfig> targetWrapper = new LambdaUpdateWrapper<>();
        targetWrapper.eq(LlmConfig::getId, id)
                .set(LlmConfig::getIsActive, true);
        update(targetWrapper);
        invalidateActiveConfig();
        return getById(id);
    }

    @Override
    public void deleteConfig(Long id) {
        removeById(id);
        invalidateActiveConfig();
    }

}
