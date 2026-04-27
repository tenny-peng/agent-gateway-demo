package org.tenny.common.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.auth.service.SessionTokenService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final SessionTokenService sessionTokenService;

    public AuthInterceptor(SessionTokenService sessionTokenService) {
        this.sessionTokenService = sessionTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        try {
            String auth = request.getHeader("Authorization");
            AuthPrincipal principal = sessionTokenService.parseRequired(auth);
            request.setAttribute(AuthPrincipal.REQUEST_ATTR, principal);
            return true;
        } catch (Exception e) {
            writeUnauthorized(response);
            return false;
        }
    }

    private static void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"unauthorized\"}");
    }
}
