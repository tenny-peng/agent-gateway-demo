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

@Service
public class LlmConfigServiceImpl extends ServiceImpl<LlmConfigMapper, LlmConfig> implements LlmConfigService {

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
        return llmConfig;
    }

    @Override
    public LlmConfig getConfigById(Long id) {
        return getById(id);
    }

    @Override
    public LlmConfig getActiveConfig() {
        return getOne(new LambdaQueryWrapper<LlmConfig>().eq(LlmConfig::getIsActive, true));
    }

    @Override
    public LlmConfig updateConfig(Long id, LlmConfig config) {
        config.setId(id);
        updateById(config);
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
        return getById(id);
    }

    @Override
    public void deleteConfig(Long id) {
        removeById(id);
    }

}