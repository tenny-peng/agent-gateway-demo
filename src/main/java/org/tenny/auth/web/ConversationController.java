package org.tenny.auth.web;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.auth.model.SessionType;
import org.tenny.auth.service.ConversationDeleteService;
import org.tenny.auth.service.ConversationQueryService;
import org.tenny.dto.ConversationListResponse;
import org.tenny.dto.ConversationMessagesResponse;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/chat")
@Validated
public class ConversationController {

    private final ConversationQueryService conversationQueryService;
    private final ConversationDeleteService conversationDeleteService;

    public ConversationController(
            ConversationQueryService conversationQueryService,
            ConversationDeleteService conversationDeleteService) {
        this.conversationQueryService = conversationQueryService;
        this.conversationDeleteService = conversationDeleteService;
    }

    @GetMapping("/conversations")
    public ConversationListResponse listConversations(
            @RequestParam("sessionType") SessionType sessionType,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            HttpServletRequest httpRequest) {
        AuthPrincipal principal = (AuthPrincipal) httpRequest.getAttribute(AuthPrincipal.REQUEST_ATTR);
        return conversationQueryService.listConversations(principal.getUserId(), sessionType, page, pageSize);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ConversationMessagesResponse listMessages(
            @PathVariable("conversationId") String conversationId,
            @RequestParam("sessionType") SessionType sessionType,
            @RequestParam(value = "beforeSeqNo", required = false) Integer beforeSeqNo,
            @RequestParam(value = "limit", defaultValue = "200") int limit,
            HttpServletRequest httpRequest) {
        AuthPrincipal principal = (AuthPrincipal) httpRequest.getAttribute(AuthPrincipal.REQUEST_ATTR);
        return conversationQueryService.listMessages(
                principal.getUserId(), conversationId, sessionType, beforeSeqNo, limit);
    }

    /**
     * Delete a conversation and all its messages
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable("conversationId") String conversationId,
            @RequestParam("sessionType") SessionType sessionType,
            HttpServletRequest httpRequest) {
        
        AuthPrincipal principal = (AuthPrincipal) httpRequest.getAttribute(AuthPrincipal.REQUEST_ATTR);
        long userId = principal.getUserId();
        
        conversationDeleteService.deleteConversation(userId, conversationId, sessionType.name());
        
        return ResponseEntity.ok().build();
    }
}
