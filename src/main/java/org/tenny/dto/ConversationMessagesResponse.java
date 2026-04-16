package org.tenny.dto;

import java.util.List;

public class ConversationMessagesResponse {

    private String conversationId;
    private String sessionType;
    private List<MessageItem> items;

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

    public List<MessageItem> getItems() {
        return items;
    }

    public void setItems(List<MessageItem> items) {
        this.items = items;
    }

    public static class MessageItem {
        private int seqNo;
        private String role;
        private String content;
        private String createdAt;

        public int getSeqNo() {
            return seqNo;
        }

        public void setSeqNo(int seqNo) {
            this.seqNo = seqNo;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}
