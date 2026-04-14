package org.tenny.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.tenny.auth.entity.UserConversation;

public interface UserConversationMapper extends BaseMapper<UserConversation> {

    /**
     * Idempotent: duplicate (user, conversation, type) ignored by unique index.
     */
    @Insert("INSERT IGNORE INTO user_conversation (user_id, conversation_id, session_type) "
            + "VALUES (#{userId}, #{conversationId}, #{sessionType})")
    int insertIgnore(@Param("userId") long userId,
                     @Param("conversationId") String conversationId,
                     @Param("sessionType") String sessionType);
}
