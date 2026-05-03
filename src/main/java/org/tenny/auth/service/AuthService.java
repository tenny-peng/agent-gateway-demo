package org.tenny.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tenny.auth.dto.AuthResponse;
import org.tenny.auth.dto.LoginRequest;
import org.tenny.auth.dto.RegisterRequest;
import org.tenny.user.entity.AppUser;
import org.tenny.user.mapper.AppUserMapper;
import org.tenny.common.config.AppProperties;
import org.tenny.common.exception.UnauthorizedException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;
    private final AppProperties appProperties;

    public AuthResponse register(RegisterRequest request) {
        String uname = request.getUsername().trim();
        Long c = appUserMapper.selectCount(Wrappers.lambdaQuery(AppUser.class).eq(AppUser::getUsername, uname));
        if (c != null && c > 0) {
            throw new IllegalStateException("username already exists");
        }
        AppUser u = new AppUser();
        u.setUsername(uname);
        u.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        u.setRole("USER");
        u.setChatLimitEnabled(true); // New users have chat limit enabled by default
        appUserMapper.insert(u);
        String token = sessionTokenService.createSession(u.getId(), uname, "USER");
        return new AuthResponse(
                token,
                appProperties.getSecurity().getSessionExpireHours(),
                uname,
                "USER");
    }

    public AuthResponse login(LoginRequest request) {
        String uname = request.getUsername().trim();
        AppUser row = appUserMapper.selectOne(
                Wrappers.lambdaQuery(AppUser.class).eq(AppUser::getUsername, uname));
        if (row == null) {
            throw new UnauthorizedException("invalid credentials");
        }
        if (!passwordEncoder.matches(request.getPassword(), row.getPasswordHash())) {
            throw new UnauthorizedException("invalid credentials");
        }
        String token = sessionTokenService.createSession(row.getId(), row.getUsername(), row.getRole());
        return new AuthResponse(
                token,
                appProperties.getSecurity().getSessionExpireHours(),
                row.getUsername(),
                row.getRole());
    }
}
