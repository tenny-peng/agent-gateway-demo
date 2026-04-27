package org.tenny.skill.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tenny.skill.dto.AdminSkillDto;
import org.tenny.skill.dto.AdminSkillProjection;
import org.tenny.skill.dto.SkillCreateRequest;
import org.tenny.skill.dto.SkillUpdateRequest;
import org.tenny.skill.dto.SkillVo;
import org.tenny.skill.entity.UserSkill;
import org.tenny.skill.mapper.UserSkillMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user skills (CRUD operations).
 */
@Service
public class SkillService {

    private static final Logger log = LoggerFactory.getLogger(SkillService.class);

    private final UserSkillMapper userSkillMapper;

    public SkillService(UserSkillMapper userSkillMapper) {
        this.userSkillMapper = userSkillMapper;
    }

    /**
     * Create a new skill for the user.
     */
    @Transactional
    public SkillVo createSkill(Long userId, SkillCreateRequest request) {
        UserSkill skill = new UserSkill();
        skill.setUserId(userId);
        skill.setTitle(request.getTitle());
        skill.setDescription(request.getDescription());
        skill.setContent(request.getContent());
        skill.setIsActive(true);
        skill.setCreatedAt(LocalDateTime.now());
        skill.setUpdatedAt(LocalDateTime.now());

        userSkillMapper.insert(skill);
        log.info("Created skill {} for user {}", skill.getId(), userId);

        return SkillVo.fromEntity(skill);
    }

    /**
     * Update an existing skill.
     */
    @Transactional
    public SkillVo updateSkill(Long userId, Long skillId, SkillUpdateRequest request) {
        UserSkill skill = userSkillMapper.selectById(skillId);
        if (skill == null) {
            throw new RuntimeException("Skill not found: " + skillId);
        }
        if (!skill.getUserId().equals(userId)) {
            throw new RuntimeException("Skill does not belong to user: " + userId);
        }

        if (request.getTitle() != null) {
            skill.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            skill.setDescription(request.getDescription());
        }
        if (request.getContent() != null) {
            skill.setContent(request.getContent());
        }
        if (request.getIsActive() != null) {
            skill.setIsActive(request.getIsActive());
        }
        skill.setUpdatedAt(LocalDateTime.now());

        userSkillMapper.updateById(skill);
        log.info("Updated skill {} for user {}", skillId, userId);

        return SkillVo.fromEntity(skill);
    }

    /**
     * Delete a skill.
     */
    @Transactional
    public void deleteSkill(Long userId, Long skillId) {
        UserSkill skill = userSkillMapper.selectById(skillId);
        if (skill == null) {
            throw new RuntimeException("Skill not found: " + skillId);
        }
        if (!skill.getUserId().equals(userId)) {
            throw new RuntimeException("Skill does not belong to user: " + userId);
        }

        userSkillMapper.deleteById(skillId);
        log.info("Deleted skill {} for user {}", skillId, userId);
    }

    /**
     * Get all active skills for a user.
     */
    public List<SkillVo> getActiveSkills(Long userId) {
        List<UserSkill> skills = userSkillMapper.selectActiveByUserId(userId);
        return skills.stream()
                .map(SkillVo::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all skills for a user (including inactive).
     */
    public List<SkillVo> getAllSkills(Long userId) {
        List<UserSkill> skills = userSkillMapper.selectAllByUserId(userId);
        return skills.stream()
                .map(SkillVo::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get a single skill by ID.
     */
    public SkillVo getSkill(Long userId, Long skillId) {
        UserSkill skill = userSkillMapper.selectById(skillId);
        if (skill == null) {
            throw new RuntimeException("Skill not found: " + skillId);
        }
        if (!skill.getUserId().equals(userId)) {
            throw new RuntimeException("Skill does not belong to user: " + userId);
        }
        return SkillVo.fromEntity(skill);
    }

    /**
     * Toggle skill active status.
     */
    @Transactional
    public SkillVo toggleSkill(Long userId, Long skillId) {
        UserSkill skill = userSkillMapper.selectById(skillId);
        if (skill == null) {
            throw new RuntimeException("Skill not found: " + skillId);
        }
        if (!skill.getUserId().equals(userId)) {
            throw new RuntimeException("Skill does not belong to user: " + userId);
        }

        skill.setIsActive(!skill.getIsActive());
        skill.setUpdatedAt(LocalDateTime.now());
        userSkillMapper.updateById(skill);

        log.info("Toggled skill {} to {} for user {}", skillId, skill.getIsActive(), userId);
        return SkillVo.fromEntity(skill);
    }

    /**
     * Get all skills across all users (admin only).
     */
    public List<AdminSkillDto> getAllSkillsForAllUsers() {
        return userSkillMapper.selectAllWithUser().stream()
                .map(this::convertToAdminSkillDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert projection to AdminSkillDto.
     */
    private AdminSkillDto convertToAdminSkillDto(AdminSkillProjection p) {
        AdminSkillDto dto = new AdminSkillDto();
        dto.setId(p.getId());
        dto.setUserId(p.getUserId());
        dto.setUsername(p.getUsername());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());
        dto.setContent(p.getContent());
        dto.setIsActive(p.getIsActive());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}