package org.tenny.auth.dto;

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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiresInHours() {
        return expiresInHours;
    }

    public void setExpiresInHours(long expiresInHours) {
        this.expiresInHours = expiresInHours;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
