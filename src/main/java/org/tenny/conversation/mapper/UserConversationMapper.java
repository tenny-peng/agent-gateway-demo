package org.tenny.conversation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.tenny.conversation.entity.UserConversation;
import org.tenny.conversation.dto.ConversationSummaryVo;

import java.util.List;

public interface UserConversationMapper extends BaseMapper<UserConversation> {

    /**
     * Idempotent: duplicate (user, conversation, type) ignored by unique index.
     */
    @Insert("INSERT IGNORE INTO user_conversation (user_id, conversation_id, session_type) "
            + "VALUES (#{userId}, #{conversationId}, #{sessionType})")
    int insertIgnore(@Param("userId") long userId,
                     @Param("conversationId") String conversationId,
                     @Param("sessionType") String sessionType);

    @Update("UPDATE user_conversation "
            + "SET title = CASE WHEN (title IS NULL OR title = '') THEN #{title} ELSE title END, "
            + "last_message_at = CURRENT_TIMESTAMP "
            + "WHERE user_id = #{userId} AND conversation_id = #{conversationId} AND session_type = #{sessionType}")
    int touchConversation(@Param("userId") long userId,
                          @Param("conversationId") String conversationId,
                          @Param("sessionType") String sessionType,
                          @Param("title") String title);

    @Select("SELECT uc.conversation_id AS conversationId, uc.session_type AS sessionType, uc.title AS title, "
            + "uc.first_seen_at AS firstSeenAt, uc.last_message_at AS lastMessageAt, "
            + "COALESCE(msg.cnt, 0) AS messageCount "
            + "FROM user_conversation uc "
            + "LEFT JOIN ("
            + "  SELECT user_id, conversation_id, session_type, COUNT(*) AS cnt "
            + "  FROM user_conversation_message GROUP BY user_id, conversation_id, session_type"
            + ") msg ON msg.user_id = uc.user_id "
            + " AND msg.conversation_id = uc.conversation_id "
            + " AND msg.session_type = uc.session_type "
            + "WHERE uc.user_id = #{userId} AND uc.session_type = #{sessionType} "
            + "ORDER BY COALESCE(uc.last_message_at, uc.first_seen_at) DESC "
            + "LIMIT #{limit} OFFSET #{offset}")
    List<ConversationSummaryVo> selectSummaries(@Param("userId") long userId,
                                                @Param("sessionType") String sessionType,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM user_conversation WHERE user_id = #{userId} AND session_type = #{sessionType}")
    long countSummaries(@Param("userId") long userId,
                        @Param("sessionType") String sessionType);

    /**
     * Delete conversation and all its messages for a user
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM user_conversation " +
            "WHERE user_id = #{userId} AND conversation_id = #{conversationId} AND session_type = #{sessionType}")
    int deleteConversation(@Param("userId") long userId,
                           @Param("conversationId") String conversationId,
                           @Param("sessionType") String sessionType);
}
