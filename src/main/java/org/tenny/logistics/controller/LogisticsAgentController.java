package org.tenny.logistics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.logistics.dto.AgentChatResponse;
import org.tenny.generic.dto.ChatRequest;
import org.tenny.logistics.service.LogisticsAgentService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Logistics business agent (waybill tool): {@code /api/logistics/agent/...}.
 */
@RestController
@RequestMapping("/api/logistics/agent")
@Validated
public class LogisticsAgentController {

    private final LogisticsAgentService logisticsAgentService;
    private final ObjectMapper objectMapper;

    public LogisticsAgentController(LogisticsAgentService logisticsAgentService, ObjectMapper objectMapper) {
        this.logisticsAgentService = logisticsAgentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/chat")
    public AgentChatResponse chat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal principal = (AuthPrincipal) httpRequest.getAttribute(AuthPrincipal.REQUEST_ATTR);
        return logisticsAgentService.run(request.getMessage(), request.getConversationId(), principal.getUserId());
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        SseEmitter emitter = new SseEmitter(0L);
        MediaType jsonUtf8 = MediaType.parseMediaType("application/json;charset=UTF-8");
        AuthPrincipal principal = (AuthPrincipal) httpRequest.getAttribute(AuthPrincipal.REQUEST_ATTR);
        
        // 设置超时回调（客户端断开时调用）
        emitter.onTimeout(() -> {
            System.out.println("[Stream] Client disconnected (timeout) - conversationId: " + request.getConversationId());
            emitter.complete();
        });
        
        // 设置完成回调
        emitter.onCompletion(() -> {
            System.out.println("[Stream] Stream completed - conversationId: " + request.getConversationId());
        });
        
        // 设置错误回调
        emitter.onError(throwable -> {
            System.out.println("[Stream] Stream error: " + throwable.getMessage() + " - conversationId: " + request.getConversationId());
        });
        
        // 用于跟踪是否已被中断
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        
        CompletableFuture.runAsync(() -> {
            try {
                logisticsAgentService.runStream(request.getMessage(), request.getConversationId(),
                        principal.getUserId(), emitter, isCompleted);
            } catch (Exception e) {
                if (!"Stream interrupted".equals(e.getMessage())) {
                    try {
                        Map<String, String> errEvt = new LinkedHashMap<String, String>();
                        errEvt.put("type", "error");
                        errEvt.put("text", e.getMessage() != null ? e.getMessage() : "Unknown error");
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(errEvt), jsonUtf8));
                    } catch (Exception ignored) {
                    }
                    emitter.complete();
                } else {
                    emitter.complete();
                }
            } finally {
                isCompleted.set(true);
            }
        });
        return emitter;
    }
}
