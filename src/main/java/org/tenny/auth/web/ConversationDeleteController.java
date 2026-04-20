package org.tenny.auth.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.auth.model.SessionType;
import org.tenny.auth.service.ConversationDeleteService;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/chat")
public class ConversationDeleteController {

    private final ConversationDeleteService conversationDeleteService;

    public ConversationDeleteController(ConversationDeleteService conversationDeleteService) {
        this.conversationDeleteService = conversationDeleteService;
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