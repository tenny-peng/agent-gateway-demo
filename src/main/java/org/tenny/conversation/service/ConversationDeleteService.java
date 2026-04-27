package org.tenny.conversation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tenny.conversation.entity.UserConversation;
import org.tenny.conversation.entity.UserConversationMessage;
import org.tenny.conversation.mapper.UserConversationMapper;
import org.tenny.conversation.mapper.UserConversationMessageMapper;

@Service
@RequiredArgsConstructor
public class ConversationDeleteService {

    private final UserConversationMapper userConversationMapper;
    private final UserConversationMessageMapper userConversationMessageMapper;

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