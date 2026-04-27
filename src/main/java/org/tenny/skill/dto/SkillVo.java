package org.tenny.skill.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * View object for returning skill data to the client.
 */
@Setter
@Getter
public class SkillVo {

    private Long id;

    private String title;

    private String description;

    private String content;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static SkillVo fromEntity(org.tenny.skill.entity.UserSkill entity) {
        if (entity == null) {
            return null;
        }
        SkillVo vo = new SkillVo();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setDescription(entity.getDescription());
        vo.setContent(entity.getContent());
        vo.setIsActive(entity.getIsActive());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
