package org.tenny.auth.web;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tenny.auth.dto.AuthResponse;
import org.tenny.auth.dto.LoginRequest;
import org.tenny.auth.dto.MeResponse;
import org.tenny.auth.dto.RegisterRequest;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.auth.service.AuthService;
import org.tenny.auth.service.SessionTokenService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;
    private final SessionTokenService sessionTokenService;

    public AuthController(AuthService authService, SessionTokenService sessionTokenService) {
        this.authService = authService;
        this.sessionTokenService = sessionTokenService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public MeResponse me(HttpServletRequest request) {
        AuthPrincipal p = (AuthPrincipal) request.getAttribute(AuthPrincipal.REQUEST_ATTR);
        return new MeResponse(p.getUserId(), p.getUsername(), p.getRole());
    }

    /**
     * Removes the session key from Redis (requires a valid Bearer token).
     */
    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        sessionTokenService.revokeBearer(request.getHeader("Authorization"));
    }
}
