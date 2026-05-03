package org.tenny.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MeResponse {

    private long userId;
    private String username;
    private String role;

    public MeResponse() {
    }

    public MeResponse(long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

}
