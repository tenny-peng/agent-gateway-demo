package org.tenny.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Admin view of a skill that includes the owner's username.
 */
@Setter
@Getter
public class AdminSkillDto {
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