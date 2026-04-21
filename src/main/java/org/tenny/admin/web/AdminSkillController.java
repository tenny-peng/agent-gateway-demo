package org.tenny.admin.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.admin.dto.AdminSkillDto;
import org.tenny.skill.service.SkillService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Admin-only controller for managing skills across all users.
 */
@RestController
@RequestMapping("/api/admin/skills")
public class AdminSkillController {

    private final SkillService skillService;

    public AdminSkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    /**
     * Get all skills across all users (admin only).
     */
    @GetMapping
    public List<AdminSkillDto> listAllSkills(HttpServletRequest request) {
        AuthPrincipal p = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        if (p == null || !p.isAdmin()) {
            throw new org.tenny.web.ForbiddenException("admin only");
        }
        return skillService.getAllSkillsForAllUsers();
    }
}