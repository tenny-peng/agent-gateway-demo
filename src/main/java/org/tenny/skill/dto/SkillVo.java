package org.tenny.skill.dto;

import java.time.LocalDateTime;

/**
 * View object for returning skill data to the client.
 */
public class SkillVo {

    private Long id;

    private String title;

    private String description;

    private String content;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

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
