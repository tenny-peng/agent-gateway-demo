package org.tenny.llmconfig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * LLM Configuration entity - stores AI model configuration settings.
 * Supports multiple configurations with one active at a time for dynamic model switching.
 */
@Setter
@Getter
@TableName("llm_config")
public class LlmConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("base_url")
    private String baseUrl;

    @TableField("api_key")
    private String apiKey;

    private String model;

    @TableField("timeout_ms")
    private Integer timeoutMs;

    @TableField("stream_timeout_ms")
    private Integer streamTimeoutMs;

    @TableField("is_active")
    private Boolean isActive;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}