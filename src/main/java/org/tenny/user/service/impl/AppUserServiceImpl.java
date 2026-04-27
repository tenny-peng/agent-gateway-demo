package org.tenny.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.tenny.user.entity.AppUser;
import org.tenny.user.mapper.AppUserMapper;
import org.tenny.user.service.AppUserService;

@Service
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements AppUserService {
}
