package org.tenny.admin.web;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.tenny.auth.entity.AppUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tenny.admin.dto.AdminStatsResponse;
import org.tenny.auth.mapper.AppUserMapper;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.auth.model.UserSessionStatsVo;
import org.tenny.web.ForbiddenException;

import javax.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final DateTimeFormatter CREATED = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AppUserMapper appUserMapper;

    public AdminController(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    @GetMapping("/stats")
    public AdminStatsResponse stats(HttpServletRequest request) {
        AuthPrincipal p = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        if (p == null || !p.isAdmin()) {
            throw new ForbiddenException("admin only");
        }
        long total = appUserMapper.selectCount(Wrappers.lambdaQuery(AppUser.class));
        List<UserSessionStatsVo> rows = appUserMapper.selectUserSessionStats();
        List<AdminStatsResponse.UserWithSessions> out = new ArrayList<AdminStatsResponse.UserWithSessions>();
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
        return new AdminStatsResponse(total, out);
    }
}
