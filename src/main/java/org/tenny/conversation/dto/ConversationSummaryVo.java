package org.tenny.conversation.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class ConversationSummaryVo {

    private String conversationId;
    private String sessionType;
    private String title;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastMessageAt;
    private long messageCount;

}
