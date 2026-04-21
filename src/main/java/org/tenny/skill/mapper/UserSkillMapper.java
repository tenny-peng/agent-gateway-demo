package org.tenny.skill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.tenny.skill.entity.UserSkill;
import org.tenny.skill.dto.AdminSkillProjection;

import java.util.List;

/**
 * Mapper for UserSkill entity.
 */
public interface UserSkillMapper extends BaseMapper<UserSkill> {

    /**
     * Get all active skills for a user.
     */
    @Select("SELECT * FROM user_skill WHERE user_id = #{userId} AND is_active = 1 ORDER BY created_at DESC")
    List<UserSkill> selectActiveByUserId(Long userId);

    /**
     * Get all skills for a user (including inactive).
     */
    @Select("SELECT * FROM user_skill WHERE user_id = #{userId} ORDER BY is_active DESC, created_at DESC")
    List<UserSkill> selectAllByUserId(Long userId);

    /**
     * Get all skills across all users with username (admin view).
     * Returns a projection of user_skill joined with app_user.
     */
    @Select("SELECT us.id, us.user_id, u.username, us.title, us.description, us.content, " +
            "us.is_active, us.created_at, us.updated_at " +
            "FROM user_skill us " +
            "JOIN app_user u ON us.user_id = u.id " +
            "ORDER BY us.created_at DESC")
    List<AdminSkillProjection> selectAllWithUser();
}
