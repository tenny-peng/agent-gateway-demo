package org.tenny.auth.model;

public final class AuthPrincipal {

    public static final String REQUEST_ATTR = "authPrincipal";

    private final long userId;
    private final String username;
    private final String role;

    public AuthPrincipal(long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
