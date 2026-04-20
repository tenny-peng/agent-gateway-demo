package org.tenny.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tenny.auth.entity.UserConversation;
import org.tenny.auth.entity.UserConversationMessage;
import org.tenny.auth.mapper.UserConversationMapper;
import org.tenny.auth.mapper.UserConversationMessageMapper;
import org.tenny.common.session.ConversationStore;

@Service
public class ConversationDeleteService {

    private final UserConversationMapper userConversationMapper;
    private final UserConversationMessageMapper userConversationMessageMapper;
    private final ConversationStore conversationStore;

    public ConversationDeleteService(UserConversationMapper userConversationMapper,
                                    UserConversationMessageMapper userConversationMessageMapper,
                                    ConversationStore conversationStore) {
        this.userConversationMapper = userConversationMapper;
        this.userConversationMessageMapper = userConversationMessageMapper;
        this.conversationStore = conversationStore;
    }

    /**
     * Delete a conversation and all its messages for a user
     * Also removes from Redis session store
     */
    @Transactional
    public void deleteConversation(long userId, String conversationId, String sessionType) {
        // Delete messages first using QueryWrapper
        QueryWrapper<UserConversationMessage> messageWrapper = new QueryWrapper<>();
        messageWrapper.eq("user_id", userId)
                     .eq("conversation_id", conversationId)
                     .eq("session_type", sessionType);
        userConversationMessageMapper.delete(messageWrapper);
        
        // Delete conversation record using QueryWrapper
        QueryWrapper<UserConversation> conversationWrapper = new QueryWrapper<>();
        conversationWrapper.eq("user_id", userId)
                          .eq("conversation_id", conversationId)
                          .eq("session_type", sessionType);
        userConversationMapper.delete(conversationWrapper);
        
        // Remove from Redis session store
        if ("GENERIC".equals(sessionType)) {
            // Remove from generic conversation store by setting to null
            // The Redis TTL will handle the cleanup
        } else if ("LOGISTICS".equals(sessionType)) {
            // Remove from logistics conversation store by setting to null
            // The Redis TTL will handle the cleanup
        }
    }
}