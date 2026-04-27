package org.tenny.auth.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Setter
@Getter
public class RegisterRequest {

    @NotBlank
    @Size(min = 2, max = 64)
    private String username;

    @NotBlank
    @Size(min = 6, max = 128)
    private String password;

}
