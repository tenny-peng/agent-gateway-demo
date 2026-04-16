package org.tenny.auth.service;

import org.springframework.stereotype.Service;
import org.tenny.auth.entity.UserConversationMessage;
import org.tenny.auth.mapper.UserConversationMapper;
import org.tenny.auth.mapper.UserConversationMessageMapper;
import org.tenny.auth.model.ConversationSummaryVo;
import org.tenny.auth.model.SessionType;
import org.tenny.dto.ConversationListResponse;
import org.tenny.dto.ConversationMessagesResponse;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationQueryService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserConversationMapper userConversationMapper;
    private final UserConversationMessageMapper userConversationMessageMapper;

    public ConversationQueryService(UserConversationMapper userConversationMapper,
                                    UserConversationMessageMapper userConversationMessageMapper) {
        this.userConversationMapper = userConversationMapper;
        this.userConversationMessageMapper = userConversationMessageMapper;
    }

    public ConversationListResponse listConversations(long userId, SessionType sessionType, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        int offset = (safePage - 1) * safePageSize;
        List<ConversationSummaryVo> rows = userConversationMapper.selectSummaries(
                userId, sessionType.name(), offset, safePageSize);
        long total = userConversationMapper.countSummaries(userId, sessionType.name());

        List<ConversationListResponse.ConversationSummaryItem> items =
                new ArrayList<ConversationListResponse.ConversationSummaryItem>();
        for (ConversationSummaryVo row : rows) {
            ConversationListResponse.ConversationSummaryItem item = new ConversationListResponse.ConversationSummaryItem();
            item.setConversationId(row.getConversationId());
            item.setSessionType(row.getSessionType());
            item.setTitle((row.getTitle() == null || row.getTitle().trim().isEmpty()) ? "新会话" : row.getTitle());
            item.setMessageCount(row.getMessageCount());
            item.setFirstSeenAt(row.getFirstSeenAt() == null ? null : TS_FMT.format(row.getFirstSeenAt()));
            item.setLastMessageAt(row.getLastMessageAt() == null ? null : TS_FMT.format(row.getLastMessageAt()));
            items.add(item);
        }

        ConversationListResponse resp = new ConversationListResponse();
        resp.setItems(items);
        resp.setPage(safePage);
        resp.setPageSize(safePageSize);
        resp.setTotal(total);
        return resp;
    }

    public ConversationMessagesResponse listMessages(long userId, String conversationId,
                                                     SessionType sessionType, Integer beforeSeqNo, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        List<UserConversationMessage> rows;
        if (beforeSeqNo == null || beforeSeqNo.intValue() <= 0) {
            rows = userConversationMessageMapper.selectMessages(userId, conversationId, sessionType.name(), safeLimit);
        } else {
            rows = userConversationMessageMapper.selectMessagesBefore(
                    userId, conversationId, sessionType.name(), beforeSeqNo.intValue(), safeLimit);
        }

        List<ConversationMessagesResponse.MessageItem> items =
                new ArrayList<ConversationMessagesResponse.MessageItem>();
        for (UserConversationMessage row : rows) {
            ConversationMessagesResponse.MessageItem item = new ConversationMessagesResponse.MessageItem();
            item.setSeqNo(row.getSeqNo() == null ? 0 : row.getSeqNo().intValue());
            item.setRole(row.getRole());
            item.setContent(row.getContent());
            item.setCreatedAt(row.getCreatedAt() == null ? null : TS_FMT.format(row.getCreatedAt()));
            items.add(item);
        }

        ConversationMessagesResponse resp = new ConversationMessagesResponse();
        resp.setConversationId(conversationId);
        resp.setSessionType(sessionType.name());
        resp.setItems(items);
        return resp;
    }
}
