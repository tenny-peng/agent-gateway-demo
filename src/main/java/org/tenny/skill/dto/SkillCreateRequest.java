package org.tenny.skill.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Request DTO for creating a new skill.
 */
@Setter
@Getter
public class SkillCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    @NotBlank(message = "Content is required")
    private String content;

}
