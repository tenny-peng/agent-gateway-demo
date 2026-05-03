package org.tenny.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthResponse {

    private String token;
    private long expiresInHours;
    private String username;
    private String role;

    public AuthResponse() {
    }

    public AuthResponse(String token, long expiresInHours, String username, String role) {
        this.token = token;
        this.expiresInHours = expiresInHours;
        this.username = username;
        this.role = role;
    }

}
