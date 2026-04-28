package org.tenny.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RequireAdminInterceptor requireAdminInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor, RequireAdminInterceptor requireAdminInterceptor) {
        this.authInterceptor = authInterceptor;
        this.requireAdminInterceptor = requireAdminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/register", "/api/auth/login");
        registry.addInterceptor(requireAdminInterceptor)
                .addPathPatterns("/api/**");
    }
}
