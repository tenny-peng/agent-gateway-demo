package org.tenny.skill.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Projection DTO for admin skill queries with user information.
 */
@Setter
@Getter
public class AdminSkillProjection {
    private Long id;
    private Long userId;
    private String username;
    private String title;
    private String description;
    private String content;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}