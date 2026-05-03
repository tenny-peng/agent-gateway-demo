package org.tenny.conversation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@TableName("user_conversation_message")
public class UserConversationMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("session_type")
    private String sessionType;

    @TableField("seq_no")
    private Integer seqNo;

    @TableField("role")
    private String role;

    @TableField("content")
    private String content;

    @TableField("reasoning")
    private String reasoning;

    @TableField("tool_name")
    private String toolName;

    @TableField("created_at")
    private LocalDateTime createdAt;

}
