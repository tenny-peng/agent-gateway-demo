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
        CompletableFuture.runAsync(() -> {
            try {
                GenericChatService.StreamChatContext ctx = genericChatService.prepareStreamContext(
                        request.getMessage(), request.getConversationId(), principal.getUserId());
                String meta = "{\"conversationId\":\"" + ctx.getConversationId() + "\"}";
                emitter.send(SseEmitter.event().data(meta, jsonUtf8));
                genericChatService.streamWithContext(ctx, piece ->
                        emitter.send(SseEmitter.event().data(piece, textUtf8)));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
