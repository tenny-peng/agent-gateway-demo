package org.tenny.logistics.web;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.dto.AgentChatResponse;
import org.tenny.dto.ChatRequest;
import org.tenny.logistics.service.LogisticsAgentService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.concurrent.CompletableFuture;

/**
 * Logistics business agent (waybill tool): {@code /api/logistics/agent/...}.
 */
@RestController
@RequestMapping("/api/logistics/agent")
@Validated
public class LogisticsAgentController {

    private final LogisticsAgentService logisticsAgentService;

    public LogisticsAgentController(LogisticsAgentService logisticsAgentService) {
        this.logisticsAgentService = logisticsAgentService;
    }

    @PostMapping("/chat")
    public AgentChatResponse chat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal principal = (AuthPrincipal) httpRequest.getAttribute(AuthPrincipal.REQUEST_ATTR);
        return logisticsAgentService.run(request.getMessage(), request.getConversationId(), principal.getUserId());
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        SseEmitter emitter = new SseEmitter(0L);
        AuthPrincipal principal = (AuthPrincipal) httpRequest.getAttribute(AuthPrincipal.REQUEST_ATTR);
        CompletableFuture.runAsync(() -> {
            try {
                logisticsAgentService.runStream(request.getMessage(), request.getConversationId(),
                        principal.getUserId(), emitter);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
