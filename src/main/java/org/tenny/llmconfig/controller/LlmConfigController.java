package org.tenny.llmconfig.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.exception.ForbiddenException;
import org.tenny.llmconfig.entity.LlmConfig;
import org.tenny.llmconfig.service.LlmConfigService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Admin controller for managing LLM configurations.
 */
@RestController
@RequestMapping("/api/admin/llm-configs")
@RequiredArgsConstructor
public class LlmConfigController {

    private final LlmConfigService llmConfigService;

    /**
     * Get all LLM configurations.
     */
    @GetMapping
    public List<LlmConfig> getAllConfigs(HttpServletRequest request) {
        checkAdminPermission(request);
        return llmConfigService.list();
    }

    /**
     * Get a specific LLM configuration by ID.
     */
    @GetMapping("/{id}")
    public LlmConfig getConfig(@PathVariable Long id, HttpServletRequest request) {
        checkAdminPermission(request);
        return llmConfigService.getConfigById(id);
    }

    /**
     * Get the active LLM configuration.
     */
    @GetMapping("/active")
    public LlmConfig getActiveConfig(HttpServletRequest request) {
        checkAdminPermission(request);
        return llmConfigService.getActiveConfig();
    }

    /**
     * Create a new LLM configuration.
     */
    @PostMapping
    public LlmConfig createConfig(@RequestBody LlmConfig config, HttpServletRequest request) {
        checkAdminPermission(request);
        return llmConfigService.add(config);
    }

    /**
     * Update an existing LLM configuration.
     */
    @PutMapping("/{id}")
    public LlmConfig updateConfig(@PathVariable Long id, @RequestBody LlmConfig config, HttpServletRequest request) {
        checkAdminPermission(request);
        return llmConfigService.updateConfig(id, config);
    }

    /**
     * Set a configuration as active.
     */
    @PostMapping("/{id}/activate")
    public LlmConfig activateConfig(@PathVariable Long id, HttpServletRequest request) {
        checkAdminPermission(request);
        return llmConfigService.setActiveConfig(id);
    }

    /**
     * Delete a LLM configuration.
     */
    @DeleteMapping("/{id}")
    public void deleteConfig(@PathVariable Long id, HttpServletRequest request) {
        checkAdminPermission(request);
        llmConfigService.deleteConfig(id);
    }

    private void checkAdminPermission(HttpServletRequest request) {
        AuthPrincipal p = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        if (p == null || !p.isAdmin()) {
            throw new ForbiddenException("admin only");
        }
    }
}