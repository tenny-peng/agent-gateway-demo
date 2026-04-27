package org.tenny.skill.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tenny.skill.dto.AdminSkillDto;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.skill.dto.SkillCreateRequest;
import org.tenny.skill.dto.SkillUpdateRequest;
import org.tenny.skill.dto.SkillVo;
import org.tenny.skill.service.SkillService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * REST controller for managing user skills.
 */
@RestController
@RequestMapping("/api/skills")
@Validated
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    /**
     * Get all active skills for the current user.
     */
    @GetMapping
    public List<SkillVo> listSkills(HttpServletRequest request) {
        AuthPrincipal principal = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        return skillService.getActiveSkills(principal.getUserId());
    }

    /**
     * Get all skills (including inactive) for the current user.
     */
    @GetMapping("/all")
    public List<SkillVo> listAllSkills(HttpServletRequest request) {
        AuthPrincipal principal = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        return skillService.getAllSkills(principal.getUserId());
    }

    /**
     * Get a single skill by ID.
     */
    @GetMapping("/{id}")
    public SkillVo getSkill(HttpServletRequest request, @PathVariable Long id) {
        AuthPrincipal principal = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        return skillService.getSkill(principal.getUserId(), id);
    }

    /**
     * Create a new skill.
     */
    @PostMapping
    public SkillVo createSkill(HttpServletRequest request, @Valid @RequestBody SkillCreateRequest body) {
        AuthPrincipal principal = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        return skillService.createSkill(principal.getUserId(), body);
    }

    /**
     * Update an existing skill.
     */
    @PutMapping("/{id}")
    public SkillVo updateSkill(HttpServletRequest request, @PathVariable Long id, @Valid @RequestBody SkillUpdateRequest body) {
        AuthPrincipal principal = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        return skillService.updateSkill(principal.getUserId(), id, body);
    }

    /**
     * Delete a skill.
     */
    @DeleteMapping("/{id}")
    public void deleteSkill(HttpServletRequest request, @PathVariable Long id) {
        AuthPrincipal principal = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        skillService.deleteSkill(principal.getUserId(), id);
    }

    /**
     * Toggle skill active status.
     */
    @PostMapping("/{id}/toggle")
    public SkillVo toggleSkill(HttpServletRequest request, @PathVariable Long id) {
        AuthPrincipal principal = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        return skillService.toggleSkill(principal.getUserId(), id);
    }

    /**
     * Get all skills across all users (admin only).
     */
    @GetMapping("/allUsers")
    public List<AdminSkillDto> listAllUsersSkills(HttpServletRequest request) {
        AuthPrincipal p = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        if (p == null || !p.isAdmin()) {
            throw new org.tenny.exception.ForbiddenException("admin only");
        }
        return skillService.getAllSkillsForAllUsers();
    }
}
