package org.tenny.user.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MyBatis result for admin stats query.
 */
@Setter
@Getter
public class UserSessionStatsVo {

    private Long id;
    private String username;
    private String role;
    private LocalDateTime createdAt;
    private Long sessionCount;

}
