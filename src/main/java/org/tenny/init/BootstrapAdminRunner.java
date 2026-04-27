package org.tenny.init;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.tenny.user.entity.AppUser;
import org.tenny.user.mapper.AppUserMapper;
import org.tenny.config.AppProperties;

@Component
@RequiredArgsConstructor
public class BootstrapAdminRunner implements ApplicationRunner {

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    public void run(ApplicationArguments args) {
        String username = appProperties.getBootstrapAdmin().getUsername();
        String password = appProperties.getBootstrapAdmin().getPassword();
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
