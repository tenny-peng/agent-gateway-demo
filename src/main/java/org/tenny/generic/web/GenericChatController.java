package org.tenny.generic.web;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.dto.ChatRequest;
import org.tenny.dto.ChatResponse;
import org.tenny.generic.service.GenericChatService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Generic Q&amp;A (no tools): {@code /api/generic/...}.
 */
@RestController
@RequestMapping("/api/generic")
@Validated
public class GenericChatController {

    private final GenericChatService genericChatService;

    public GenericChatController(GenericChatService genericChatService) {
        this.genericChatService = genericChatService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal principal = (AuthPrincipal) httpRequest.getAttribute(AuthPrincipal.REQUEST_ATTR);
        return genericChatService.chat(request.getMessage(), request.getConversationId(), principal.getUserId());
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        SseEmitter emitter = new SseEmitter(0L);
        MediaType textUtf8 = MediaType.parseMediaType("text/plain;charset=UTF-8");
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
                GenericChatService.StreamChatContext ctx = genericChatService.prepareStreamContext(
                        request.getMessage(), request.getConversationId(), principal.getUserId());
                String meta = "{\"conversationId\":\"" + ctx.getConversationId() + "\"}";
                emitter.send(SseEmitter.event().data(meta, jsonUtf8));
                genericChatService.streamWithContext(ctx, piece -> {
                    // 检查是否已被中断
                    if (isCompleted.get()) {
                        throw new RuntimeException("Stream interrupted");
                    }
                    emitter.send(SseEmitter.event().data(piece, textUtf8));
                });
                emitter.complete();
            } catch (Exception e) {
                if (!"Stream interrupted".equals(e.getMessage())) {
                    emitter.completeWithError(e);
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
