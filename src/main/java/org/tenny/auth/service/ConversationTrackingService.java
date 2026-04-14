package org.tenny.auth.service;

import org.springframework.stereotype.Service;
import org.tenny.auth.mapper.UserConversationMapper;
import org.tenny.auth.model.SessionType;

@Service
public class ConversationTrackingService {

    private final UserConversationMapper userConversationMapper;

    public ConversationTrackingService(UserConversationMapper userConversationMapper) {
        this.userConversationMapper = userConversationMapper;
    }

    public void recordIfNew(long userId, String conversationId, SessionType sessionType) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return;
        }
        userConversationMapper.insertIgnore(userId, conversationId.trim(), sessionType.name());
    }
}
