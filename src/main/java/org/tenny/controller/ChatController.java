package org.tenny.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tenny.dto.AgentChatResponse;
import org.tenny.dto.ChatRequest;
import org.tenny.dto.ChatResponse;
import org.tenny.service.AgentService;
import org.tenny.service.ChatService;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
@Validated
public class ChatController {

    private final ChatService chatService;
    private final AgentService agentService;

    public ChatController(ChatService chatService, AgentService agentService) {
        this.chatService = chatService;
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request.getMessage());
    }

    /**
     * SSE stream of plain chat tokens (OpenAI-style deltas concatenated as text/event-stream).
     * Each event {@code data} is one UTF-8 text chunk from the model.
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        MediaType textUtf8 = MediaType.parseMediaType("text/plain;charset=UTF-8");
        CompletableFuture.runAsync(() -> {
            try {
                chatService.streamChat(request.getMessage(), piece ->
                        emitter.send(SseEmitter.event().data(piece, textUtf8)));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /** Agent: tool calling loop (demo: query_waybill). */
    @PostMapping("/agent/chat")
    public AgentChatResponse agentChat(@Valid @RequestBody ChatRequest request) {
        return agentService.run(request.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("error", e.getMessage());
        return ResponseEntity.badRequest().body(body);
    }
}
