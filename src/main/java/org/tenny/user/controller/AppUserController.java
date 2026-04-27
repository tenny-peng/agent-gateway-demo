package org.tenny.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tenny.user.dto.AdminStatsResponse;
import org.tenny.user.entity.AppUser;
import org.tenny.user.mapper.AppUserMapper;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.user.dto.UserSessionStatsVo;
import org.tenny.common.exception.ForbiddenException;

import javax.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AppUserController {

    private static final DateTimeFormatter CREATED = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AppUserMapper appUserMapper;

    @GetMapping("/users")
    public List<AdminStatsResponse.UserWithSessions> listUsers(HttpServletRequest request) {
        AuthPrincipal p = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        if (p == null || !p.isAdmin()) {
            throw new ForbiddenException("admin only");
        }
        List<UserSessionStatsVo> rows = appUserMapper.selectUserSessionStats();
        List<AdminStatsResponse.UserWithSessions> out = new ArrayList<>();
        for (UserSessionStatsVo r : rows) {
            String created = r.getCreatedAt() == null
                    ? ""
                    : CREATED.format(r.getCreatedAt());
            out.add(new AdminStatsResponse.UserWithSessions(
                    r.getId(),
                    r.getUsername(),
                    r.getRole(),
                    created,
                    r.getSessionCount() == null ? 0L : r.getSessionCount()));
        }
        return out;
    }

    @GetMapping("/users/{userId}/chat-limit")
    public ResponseEntity<Map<String, Object>> getChatLimit(@PathVariable("userId") Long userId,
                                                           HttpServletRequest request) {
        AuthPrincipal p = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        if (p == null || !p.isAdmin()) {
            throw new ForbiddenException("admin only");
        }
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("limitEnabled", Boolean.TRUE.equals(user.getChatLimitEnabled()));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{userId}/chat-limit")
    public void updateChatLimit(@PathVariable("userId") Long userId,
                               @RequestParam("limitEnabled") Boolean limitEnabled,
                               HttpServletRequest request) {
        AuthPrincipal p = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        if (p == null || !p.isAdmin()) {
            throw new ForbiddenException("admin only");
        }
        AppUser user = new AppUser();
        user.setId(userId);
        user.setChatLimitEnabled(limitEnabled);
        appUserMapper.updateById(user);
    }
}
