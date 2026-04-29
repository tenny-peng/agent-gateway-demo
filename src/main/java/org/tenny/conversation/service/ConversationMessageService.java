package org.tenny.conversation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tenny.conversation.mapper.UserConversationMapper;
import org.tenny.conversation.mapper.UserConversationMessageMapper;
import org.tenny.generic.enums.SessionType;

@Service
public class ConversationMessageService {

    private final UserConversationMapper userConversationMapper;
    private final UserConversationMessageMapper userConversationMessageMapper;

    public ConversationMessageService(UserConversationMapper userConversationMapper,
                                      UserConversationMessageMapper userConversationMessageMapper) {
        this.userConversationMapper = userConversationMapper;
        this.userConversationMessageMapper = userConversationMessageMapper;
    }

    @Transactional
    public void appendMessage(long userId, String conversationId, SessionType sessionType,
                              String role, String content, String toolName, String titleCandidate) {
        appendMessage(userId, conversationId, sessionType, role, content, toolName, titleCandidate, null);
    }

    @Transactional
    public void appendMessage(long userId, String conversationId, SessionType sessionType,
                              String role, String content, String toolName, String titleCandidate, String reasoning) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return;
        }
        String convId = conversationId.trim();
        String text = content == null ? "" : content;
        int nextSeqNo = userConversationMessageMapper.selectMaxSeqNo(userId, convId, sessionType.name()) + 1;
        userConversationMessageMapper.insertMessage(
                userId,
                convId,
                sessionType.name(),
                nextSeqNo,
                role,
                text,
                reasoning,
                toolName);
        userConversationMapper.touchConversation(userId, convId, sessionType.name(), normalizeTitle(titleCandidate));
    }

    private static String normalizeTitle(String text) {
        if (text == null) {
            return "";
        }
        String s = text.trim().replaceAll("\\s+", " ");
        if (s.length() <= 40) {
            return s;
        }
        return s.substring(0, 40);
    }
}
