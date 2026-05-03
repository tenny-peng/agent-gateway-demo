package org.tenny.skill.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

/**
 * Request DTO for updating an existing skill.
 */
@Setter
@Getter
public class SkillUpdateRequest {

    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    private String content;

    private Boolean isActive;

}
