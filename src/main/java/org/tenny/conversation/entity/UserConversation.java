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
@TableName("user_conversation")
public class UserConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("session_type")
    private String sessionType;

    @TableField("first_seen_at")
    private LocalDateTime firstSeenAt;

}
