package org.tenny.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tenny.auth.dto.AuthResponse;
import org.tenny.auth.dto.LoginRequest;
import org.tenny.auth.dto.RegisterRequest;
import org.tenny.auth.entity.AppUser;
import org.tenny.auth.mapper.AppUserMapper;
import org.tenny.config.AppSecurityProperties;
import org.tenny.web.UnauthorizedException;

@Service
public class AuthService {

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;
    private final AppSecurityProperties appSecurityProperties;

    public AuthService(AppUserMapper appUserMapper,
                       PasswordEncoder passwordEncoder,
                       SessionTokenService sessionTokenService,
                       AppSecurityProperties appSecurityProperties) {
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.sessionTokenService = sessionTokenService;
        this.appSecurityProperties = appSecurityProperties;
    }

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
        appUserMapper.insert(u);
        String token = sessionTokenService.createSession(u.getId(), uname, "USER");
        return new AuthResponse(
                token,
                appSecurityProperties.getSessionExpireHours(),
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
                appSecurityProperties.getSessionExpireHours(),
                row.getUsername(),
                row.getRole());
    }
}
