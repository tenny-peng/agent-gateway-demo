package org.tenny.dto;

import java.util.List;

public class ConversationListResponse {

    private List<ConversationSummaryItem> items;
    private int page;
    private int pageSize;
    private long total;

    public List<ConversationSummaryItem> getItems() {
        return items;
    }

    public void setItems(List<ConversationSummaryItem> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public static class ConversationSummaryItem {
        private String conversationId;
        private String sessionType;
        private String title;
        private String firstSeenAt;
        private String lastMessageAt;
        private long messageCount;

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public String getSessionType() {
            return sessionType;
        }

        public void setSessionType(String sessionType) {
            this.sessionType = sessionType;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getFirstSeenAt() {
            return firstSeenAt;
        }

        public void setFirstSeenAt(String firstSeenAt) {
            this.firstSeenAt = firstSeenAt;
        }

        public String getLastMessageAt() {
            return lastMessageAt;
        }

        public void setLastMessageAt(String lastMessageAt) {
            this.lastMessageAt = lastMessageAt;
        }

        public long getMessageCount() {
            return messageCount;
        }

        public void setMessageCount(long messageCount) {
            this.messageCount = messageCount;
        }
    }
}
