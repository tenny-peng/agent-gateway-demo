package org.tenny.admin.dto;

import java.util.List;

public class AdminStatsResponse {

    private long totalUsers;
    private List<UserWithSessions> users;

    public AdminStatsResponse() {
    }

    public AdminStatsResponse(long totalUsers, List<UserWithSessions> users) {
        this.totalUsers = totalUsers;
        this.users = users;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public List<UserWithSessions> getUsers() {
        return users;
    }

    public void setUsers(List<UserWithSessions> users) {
        this.users = users;
    }

    public static final class UserWithSessions {
        private long id;
        private String username;
        private String role;
        private String createdAt;
        private long sessionCount;

        public UserWithSessions() {
        }

        public UserWithSessions(long id, String username, String role, String createdAt, long sessionCount) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.createdAt = createdAt;
            this.sessionCount = sessionCount;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
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

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public long getSessionCount() {
            return sessionCount;
        }

        public void setSessionCount(long sessionCount) {
            this.sessionCount = sessionCount;
        }
    }
}
