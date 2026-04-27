package org.tenny.conversation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.tenny.conversation.entity.UserConversationMessage;

import java.util.List;

public interface UserConversationMessageMapper extends BaseMapper<UserConversationMessage> {

    @Select("SELECT COALESCE(MAX(seq_no), 0) FROM user_conversation_message "
            + "WHERE user_id = #{userId} AND conversation_id = #{conversationId} AND session_type = #{sessionType}")
    int selectMaxSeqNo(@Param("userId") long userId,
                       @Param("conversationId") String conversationId,
                       @Param("sessionType") String sessionType);

    @Insert("INSERT INTO user_conversation_message "
            + "(user_id, conversation_id, session_type, seq_no, role, content, tool_name) "
            + "VALUES (#{userId}, #{conversationId}, #{sessionType}, #{seqNo}, #{role}, #{content}, #{toolName})")
    int insertMessage(@Param("userId") long userId,
                      @Param("conversationId") String conversationId,
                      @Param("sessionType") String sessionType,
                      @Param("seqNo") int seqNo,
                      @Param("role") String role,
                      @Param("content") String content,
                      @Param("toolName") String toolName);

    @Select("SELECT id, user_id, conversation_id, session_type, seq_no, role, content, tool_name, created_at "
            + "FROM user_conversation_message "
            + "WHERE user_id = #{userId} AND conversation_id = #{conversationId} AND session_type = #{sessionType} "
            + "ORDER BY seq_no ASC LIMIT #{limit}")
    List<UserConversationMessage> selectMessages(@Param("userId") long userId,
                                                 @Param("conversationId") String conversationId,
                                                 @Param("sessionType") String sessionType,
                                                 @Param("limit") int limit);

    @Select("SELECT id, user_id, conversation_id, session_type, seq_no, role, content, tool_name, created_at "
            + "FROM user_conversation_message "
            + "WHERE user_id = #{userId} AND conversation_id = #{conversationId} AND session_type = #{sessionType} "
            + "AND seq_no < #{beforeSeqNo} ORDER BY seq_no ASC LIMIT #{limit}")
    List<UserConversationMessage> selectMessagesBefore(@Param("userId") long userId,
                                                       @Param("conversationId") String conversationId,
                                                       @Param("sessionType") String sessionType,
                                                       @Param("beforeSeqNo") int beforeSeqNo,
                                                       @Param("limit") int limit);
}
