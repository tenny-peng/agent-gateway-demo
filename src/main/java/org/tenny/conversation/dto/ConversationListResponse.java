package org.tenny.conversation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ConversationListResponse {

    private List<ConversationSummaryItem> items;
    private int page;
    private int pageSize;
    private long total;

    @Setter
    @Getter
    public static class ConversationSummaryItem {
        private String conversationId;
        private String sessionType;
        private String title;
        private String firstSeenAt;
        private String lastMessageAt;
        private long messageCount;

    }
}
