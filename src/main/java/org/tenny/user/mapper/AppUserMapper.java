package org.tenny.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.tenny.user.entity.AppUser;
import org.tenny.user.dto.UserSessionStatsVo;

import java.util.List;

public interface AppUserMapper extends BaseMapper<AppUser> {

    @Select("SELECT u.id, u.username, u.role, u.created_at AS createdAt, COALESCE(c.cnt, 0) AS sessionCount "
            + "FROM app_user u "
            + "LEFT JOIN (SELECT user_id, COUNT(*) AS cnt FROM user_conversation GROUP BY user_id) c "
            + "ON c.user_id = u.id "
            + "ORDER BY u.id")
    List<UserSessionStatsVo> selectUserSessionStats();
}
