package org.tenny.generic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tenny.auth.model.AuthPrincipal;
import org.tenny.generic.dto.ChatRequest;
import org.tenny.generic.dto.ChatResponse;
import org.tenny.generic.service.GenericChatService;

import org.tenny.common.helper.llmclient.dto.StreamChunkKind;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Generic Q&amp;A: {@code /api/generic/...}. Optional on-demand {@code web_search} tool when {@code webSearch} is true.
 */
@RestController
@RequestMapping("/api/generic")
@Validated
@RequiredArgsConstructor
public class GenericChatController {

    private final GenericChatService genericChatService;
    private final ObjectMapper objectMapper;

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal principal = (AuthPrincipal) httpRequest.getAttribute(AuthPrincipal.REQUEST_ATTR);
        return genericChatService.chat(
                request.getMessage(), request.getConversationId(), principal.getUserId(),
                request.getWebSearch(), request.getDeepThinking());
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
                GenericChatService.StreamChatContext ctx = genericChatService.prepareStreamContext(
                        request.getMessage(), request.getConversationId(), principal.getUserId(),
                        request.getWebSearch(), request.getDeepThinking());
                String meta = "{\"conversationId\":\"" + ctx.getConversationId() + "\"}";
                emitter.send(SseEmitter.event().data(meta, jsonUtf8));
                genericChatService.streamWithContext(ctx, chunk -> {
                    if (isCompleted.get()) {
                        throw new RuntimeException("Stream interrupted");
                    }
                    Map<String, String> envelope = new LinkedHashMap<String, String>();
                    envelope.put("type", chunk.getKind() == StreamChunkKind.REASONING ? "reasoning" : "content");
                    envelope.put("text", chunk.getText());
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(envelope), jsonUtf8));
                });
                emitter.complete();
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
