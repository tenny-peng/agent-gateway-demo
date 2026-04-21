package org.tenny.skill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.tenny.skill.entity.UserSkill;

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
}
