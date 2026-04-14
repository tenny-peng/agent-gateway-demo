package org.tenny.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.tenny.auth.entity.AppUser;
import org.tenny.auth.mapper.AppUserMapper;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties bootstrapAdminProperties;

    public BootstrapAdminRunner(AppUserMapper appUserMapper,
                                PasswordEncoder passwordEncoder,
                                BootstrapAdminProperties bootstrapAdminProperties) {
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapAdminProperties = bootstrapAdminProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = bootstrapAdminProperties.getUsername();
        String password = bootstrapAdminProperties.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return;
        }
        String u = username.trim();
        Long c = appUserMapper.selectCount(Wrappers.lambdaQuery(AppUser.class).eq(AppUser::getUsername, u));
        if (c != null && c > 0) {
            return;
        }
        AppUser admin = new AppUser();
        admin.setUsername(u);
        admin.setPasswordHash(passwordEncoder.encode(password.trim()));
        admin.setRole("ADMIN");
        appUserMapper.insert(admin);
    }
}
