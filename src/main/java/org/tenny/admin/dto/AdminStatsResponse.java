package org.tenny.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class AdminStatsResponse {

    private long totalUsers;
    private List<UserWithSessions> users;

    public AdminStatsResponse() {
    }

    public AdminStatsResponse(long totalUsers, List<UserWithSessions> users) {
        this.totalUsers = totalUsers;
        this.users = users;
    }

    @Setter
    @Getter
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

    }
}
