package org.tenny.generic.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tenny.user.entity.AppUser;
import org.tenny.conversation.entity.UserConversationMessage;
import org.tenny.user.mapper.AppUserMapper;
import org.tenny.conversation.mapper.UserConversationMessageMapper;
import org.tenny.generic.enums.SessionType;
import org.tenny.conversation.service.ConversationMessageService;
import org.tenny.conversation.service.ConversationTrackingService;
import org.tenny.common.helper.llmclient.LlmClient;
import org.tenny.common.helper.llmclient.LlmStreamClient;
import org.tenny.common.helper.llmclient.dto.LlmCompletionResult;
import org.tenny.common.helper.llmclient.dto.LlmToolCall;
import org.tenny.common.helper.llmclient.dto.StreamChunkKind;
import org.tenny.common.helper.llmclient.dto.StreamTextChunk;
import org.tenny.common.session.ConversationStore;
import org.tenny.generic.dto.ChatResponse;
import org.tenny.common.exception.ChatLimitExceededException;
import org.tenny.llmconfig.service.LlmConfigService;
import org.tenny.skill.service.SkillInjectService;
import org.tenny.common.config.AgentProperties;
import org.tenny.generic.tool.WebSearchQueryTool;
import org.tenny.generic.tool.WebSearchToolDefinitions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic chat: multi-turn plain LLM completion; optional {@code web_search} tool when user enables web search.
 */
@Service
@RequiredArgsConstructor
public class GenericChatService {

    private static final String CHAT_SYSTEM_BASE = "You are a helpful assistant.";

    private static final String WEB_SEARCH_SYSTEM_APPEND =
            "\n\n【联网】已为你注册 web_search 工具：仅在需要最新网页事实、新闻、价格、时效信息时调用；"
                    + "用户问候、自我介绍、纯逻辑或与网页无关的问题不要调用该工具。";

    private final LlmClient llmClient;
    private final LlmStreamClient llmStreamClient;
    private final LlmConfigService llmConfigService;
    private final ConversationStore conversationStore;
    private final SkillInjectService skillInjectService;
    private final ConversationTrackingService conversationTrackingService;
    private final ConversationMessageService conversationMessageService;
    private final UserConversationMessageMapper userConversationMessageMapper;
    private final AppUserMapper appUserMapper;
    private final AgentProperties agentProperties;
    private final WebSearchQueryTool webSearchQueryTool;

    private void checkChatLimit(long userId) {
        AppUser user = appUserMapper.selectById(userId);
        if (user != null && Boolean.TRUE.equals(user.getChatLimitEnabled())) {
            // Check message count limit (e.g., 10 messages per user)
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserConversationMessage> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            wrapper.eq("user_id", userId);
            Long messageCount = userConversationMessageMapper.selectCount(wrapper);
            if (messageCount >= 10) { // Limit to 10 messages
                throw new ChatLimitExceededException("Chat limit exceeded (10 messages). Please contact administrator to increase limit.");
            }
        }
    }

    /**
     * Plain chat. Pass {@code conversationId} from the previous {@link ChatResponse} to continue.
     *
     * @param webSearch when {@link Boolean#TRUE}, exposes {@code web_search} tool so the model may retrieve web snippets.
     * @param deepThinking when {@link Boolean#TRUE}, persists and returns model {@code reasoning_content} when present
     *        (not populated on intermediate tool rounds; with {@code webSearch} only the final completion may carry it).
     */
    public ChatResponse chat(String userMessage, String conversationId, long userId, Boolean webSearch,
                             Boolean deepThinking) {
        checkChatLimit(userId);
        String id;
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        if (conversationId == null || conversationId.trim().isEmpty()) {
            id = conversationStore.newConversationId();
            conversationTrackingService.recordIfNew(userId, id, SessionType.GENERIC);
            messages.add(chatSystem());
            messages.add(userMessage(userMessage));
        } else {
            id = conversationId.trim();
            List<Map<String, String>> previous = conversationStore.getChatMessages(id);
            if (previous == null) {
                previous = restoreChatMessages(userId, id);
            }
            messages.addAll(previous);
            messages.add(userMessage(userMessage));
        }

        skillInjectService.augmentChatSystem(messages, userMessage, userId);
        persistGenericUserTurnEarly(userId, id, userMessage, messages);

        long start = System.currentTimeMillis();
        if (Boolean.TRUE.equals(webSearch)) {
            return chatWithWebSearchTool(id, messages, userMessage, userId, deepThinking, start);
        }

        List<Map<String, String>> forLlm = messagesForLlmApi(new ArrayList<Map<String, String>>(messages));
        List<Map<String, Object>> asObject = new ArrayList<Map<String, Object>>();
        for (Map<String, String> row : forLlm) {
            asObject.add(new HashMap<String, Object>(row));
        }

        LlmCompletionResult result = llmClient.chatCompletions(asObject, null);
        long latency = System.currentTimeMillis() - start;
        if (result.hasToolCalls()) {
            throw new IllegalStateException("Model returned tool_calls but simple chat mode does not handle them");
        }
        String answer = result.getContent();
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalStateException("LLM response missing message.content");
        }
        String reasoning = Boolean.TRUE.equals(deepThinking) ? result.getReasoning() : null;

        List<Map<String, String>> toSave = new ArrayList<Map<String, String>>(messages);
        toSave.add(assistantMessage(answer, reasoning));
        skillInjectService.stripSkillsFromChatMessages(toSave);
        conversationStore.putChatMessages(id, toSave);
        conversationMessageService.appendMessage(
                userId, id, SessionType.GENERIC, "assistant", answer, null, userMessage, reasoning);

        return new ChatResponse(answer, reasoning, llmConfigService.getActiveConfig().getModel(), latency, id);
    }

    private ChatResponse chatWithWebSearchTool(String id, List<Map<String, String>> messages, String userMessage,
                                               long userId, Boolean deepThinking, long start) {
        List<Map<String, Object>> objMsgs =
                stringMessagesToObjectMessages(messagesForLlmApi(new ArrayList<Map<String, String>>(messages)));
        augmentFirstSystemForWebSearchTool(objMsgs);
        List<Map<String, Object>> tools = WebSearchToolDefinitions.webSearchToolsDefinition();
        int max = Math.max(1, agentProperties.getMaxSteps());
        int steps = 0;
        LlmCompletionResult result = null;
        while (steps < max) {
            steps++;
            result = llmClient.chatCompletions(objMsgs, tools);
            if (result.hasToolCalls()) {
                objMsgs.add(result.getAssistantMessage());
                for (LlmToolCall call : result.getToolCalls()) {
                    String payload = executeWebSearchTool(call.getName(), call.getArguments());
                    Map<String, Object> toolMsg = new HashMap<String, Object>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", call.getId());
                    toolMsg.put("content", payload);
                    objMsgs.add(toolMsg);
                }
                continue;
            }
            if (result.getContent() != null && !result.getContent().trim().isEmpty()) {
                break;
            }
            throw new IllegalStateException("LLM returned empty content and no tool_calls; raw=" + result.getAssistantMessage());
        }
        if (result == null || result.getContent() == null || result.getContent().trim().isEmpty()) {
            throw new IllegalStateException("Agent exceeded max steps (" + max + ") without final answer");
        }
        String answer = result.getContent();
        String reasoning = Boolean.TRUE.equals(deepThinking) ? result.getReasoning() : null;
        long latency = System.currentTimeMillis() - start;

        List<Map<String, String>> toSave = new ArrayList<Map<String, String>>(messages);
        toSave.add(assistantMessage(answer, reasoning));
        skillInjectService.stripSkillsFromChatMessages(toSave);
        conversationStore.putChatMessages(id, toSave);
        conversationMessageService.appendMessage(
                userId, id, SessionType.GENERIC, "assistant", answer, null, userMessage, reasoning);

        return new ChatResponse(answer, reasoning, llmConfigService.getActiveConfig().getModel(), latency, id);
    }

    private String executeWebSearchTool(String name, String argumentsJson) {
        if (WebSearchQueryTool.NAME.equals(name)) {
            return webSearchQueryTool.execute(argumentsJson);
        }
        return "{\"error\":\"unknown_tool\"}";
    }

    /**
     * Build message list for streaming; first SSE event should expose {@link StreamChatContext#getConversationId()}.
     */
    public StreamChatContext prepareStreamContext(String userMessage, String conversationId, long userId,
                                                  Boolean webSearch, Boolean deepThinking) {
        checkChatLimit(userId);
        String id;
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        if (conversationId == null || conversationId.trim().isEmpty()) {
            id = conversationStore.newConversationId();
            conversationTrackingService.recordIfNew(userId, id, SessionType.GENERIC);
            messages.add(chatSystem());
            messages.add(userMessage(userMessage));
        } else {
            id = conversationId.trim();
            List<Map<String, String>> previous = conversationStore.getChatMessages(id);
            if (previous == null) {
                previous = restoreChatMessages(userId, id);
            }
            messages.addAll(previous);
            messages.add(userMessage(userMessage));
        }
        skillInjectService.augmentChatSystem(messages, userMessage, userId);
        persistGenericUserTurnEarly(userId, id, userMessage, messages);
        return new StreamChatContext(
                id, messages, userId, userMessage, Boolean.TRUE.equals(webSearch), Boolean.TRUE.equals(deepThinking));
    }

    /**
     * Persist the user's message immediately (DB + Redis) so a refresh after backend failure still shows the question.
     * Redis copy is stripped of ephemeral skill injection to match {@link #streamWithContext} completion behavior.
     */
    private void persistGenericUserTurnEarly(long userId, String conversationId, String userMessage,
                                             List<Map<String, String>> messages) {
        conversationMessageService.appendMessage(
                userId, conversationId, SessionType.GENERIC, "user", userMessage, null, userMessage);
        List<Map<String, String>> forRedis = copyChatMessages(messages);
        skillInjectService.stripSkillsFromChatMessages(forRedis);
        conversationStore.putChatMessages(conversationId, forRedis);
    }

    private static List<Map<String, String>> copyChatMessages(List<Map<String, String>> messages) {
        List<Map<String, String>> out = new ArrayList<Map<String, String>>();
        for (Map<String, String> m : messages) {
            out.add(new HashMap<String, String>(m));
        }
        return out;
    }

    /**
     * Stream answer tokens; when {@link StreamChatContext#isDeepThinking()} also streams and persists reasoning.
     * With {@link StreamChatContext#isWebSearch()} and deep thinking, each LLM round uses
     * {@link LlmStreamClient#streamChatCompletionsWithToolsAndChunks} so reasoning and content deltas reach SSE
     * incrementally (including after tool results).
     */
    public void streamWithContext(StreamChatContext ctx, LlmStreamClient.StreamChunkConsumer onChunk) throws IOException {
        if (ctx.isWebSearch()) {
            streamWithWebSearchTools(ctx, onChunk);
            return;
        }

        List<Map<String, String>> forLlm = messagesForLlmApi(new ArrayList<Map<String, String>>(ctx.getMessages()));
        StringBuilder contentAcc = new StringBuilder();
        if (ctx.isDeepThinking()) {
            StringBuilder reasoningAcc = new StringBuilder();
            llmStreamClient.streamChatCompletionsWithChunks(forLlm, chunk -> {
                if (chunk.getKind() == StreamChunkKind.REASONING) {
                    reasoningAcc.append(chunk.getText());
                } else {
                    contentAcc.append(chunk.getText());
                }
                onChunk.onChunk(chunk);
            });
            List<Map<String, String>> next = new ArrayList<Map<String, String>>(ctx.getMessages());
            next.add(assistantMessage(
                    contentAcc.toString(), reasoningAcc.length() > 0 ? reasoningAcc.toString() : null));
            skillInjectService.stripSkillsFromChatMessages(next);
            conversationStore.putChatMessages(ctx.getConversationId(), next);
            conversationMessageService.appendMessage(
                    ctx.getUserId(),
                    ctx.getConversationId(),
                    SessionType.GENERIC,
                    "assistant",
                    contentAcc.toString(),
                    null,
                    ctx.getUserMessage(),
                    reasoningAcc.length() > 0 ? reasoningAcc.toString() : null);
        } else {
            llmStreamClient.streamChatCompletions(forLlm, piece -> {
                contentAcc.append(piece);
                onChunk.onChunk(new StreamTextChunk(StreamChunkKind.CONTENT, piece));
            });
            List<Map<String, String>> next = new ArrayList<Map<String, String>>(ctx.getMessages());
            next.add(assistantMessage(contentAcc.toString(), null));
            skillInjectService.stripSkillsFromChatMessages(next);
            conversationStore.putChatMessages(ctx.getConversationId(), next);
            conversationMessageService.appendMessage(
                    ctx.getUserId(),
                    ctx.getConversationId(),
                    SessionType.GENERIC,
                    "assistant",
                    contentAcc.toString(),
                    null,
                    ctx.getUserMessage(),
                    null);
        }
    }

    private void streamWithWebSearchTools(StreamChatContext ctx, LlmStreamClient.StreamChunkConsumer onChunk)
            throws IOException {
        List<Map<String, Object>> objMsgs =
                stringMessagesToObjectMessages(messagesForLlmApi(new ArrayList<Map<String, String>>(ctx.getMessages())));
        augmentFirstSystemForWebSearchTool(objMsgs);
        List<Map<String, Object>> tools = WebSearchToolDefinitions.webSearchToolsDefinition();
        int max = Math.max(1, agentProperties.getMaxSteps());

        if (ctx.isDeepThinking()) {
            streamWithWebSearchToolsDeepThinking(ctx, onChunk, objMsgs, tools, max);
            return;
        }

        int steps = 0;
        StringBuilder contentAcc = new StringBuilder();
        LlmCompletionResult result = null;
        while (steps < max) {
            steps++;
            result = llmStreamClient.streamChatCompletionsWithTools(objMsgs, tools, piece -> {
                contentAcc.append(piece);
                onChunk.onChunk(new StreamTextChunk(StreamChunkKind.CONTENT, piece));
            });
            if (result.hasToolCalls()) {
                objMsgs.add(result.getAssistantMessage());
                for (LlmToolCall call : result.getToolCalls()) {
                    String payload = executeWebSearchTool(call.getName(), call.getArguments());
                    Map<String, Object> toolMsg = new HashMap<String, Object>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", call.getId());
                    toolMsg.put("content", payload);
                    objMsgs.add(toolMsg);
                }
                continue;
            }
            if (result.getContent() != null && !result.getContent().trim().isEmpty()) {
                break;
            }
            throw new IllegalStateException("LLM returned empty content and no tool_calls (stream)");
        }
        if (result == null || result.getContent() == null || result.getContent().trim().isEmpty()) {
            throw new IllegalStateException("Agent exceeded max steps (" + max + ") without final answer");
        }

        List<Map<String, String>> next = new ArrayList<Map<String, String>>(ctx.getMessages());
        next.add(assistantMessage(contentAcc.toString(), null));
        skillInjectService.stripSkillsFromChatMessages(next);
        conversationStore.putChatMessages(ctx.getConversationId(), next);
        conversationMessageService.appendMessage(
                ctx.getUserId(),
                ctx.getConversationId(),
                SessionType.GENERIC,
                "assistant",
                contentAcc.toString(),
                null,
                ctx.getUserMessage(),
                null);
    }

    /**
     * Web search + deep thinking: same tool loop as streaming without deep thinking, but each LLM round uses
     * {@link LlmStreamClient#streamChatCompletionsWithToolsAndChunks} so {@code reasoning_content} and {@code content}
     * stream incrementally (no long silent sync phase).
     */
    private void streamWithWebSearchToolsDeepThinking(
            StreamChatContext ctx,
            LlmStreamClient.StreamChunkConsumer onChunk,
            List<Map<String, Object>> objMsgs,
            List<Map<String, Object>> tools,
            int max) throws IOException {
        int steps = 0;
        LlmCompletionResult result = null;
        StringBuilder contentAcc = new StringBuilder();
        StringBuilder reasoningAcc = new StringBuilder();
        while (steps < max) {
            steps++;
            contentAcc.setLength(0);
            reasoningAcc.setLength(0);
            result = llmStreamClient.streamChatCompletionsWithToolsAndChunks(objMsgs, tools, chunk -> {
                if (chunk.getKind() == StreamChunkKind.REASONING) {
                    reasoningAcc.append(chunk.getText());
                } else {
                    contentAcc.append(chunk.getText());
                }
                onChunk.onChunk(chunk);
            });
            if (result.hasToolCalls()) {
                objMsgs.add(result.getAssistantMessage());
                for (LlmToolCall call : result.getToolCalls()) {
                    String payload = executeWebSearchTool(call.getName(), call.getArguments());
                    Map<String, Object> toolMsg = new HashMap<String, Object>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", call.getId());
                    toolMsg.put("content", payload);
                    objMsgs.add(toolMsg);
                }
                continue;
            }
            if (result.getContent() != null && !result.getContent().trim().isEmpty()) {
                break;
            }
            throw new IllegalStateException("LLM returned empty content and no tool_calls (stream)");
        }
        if (result == null || result.getContent() == null || result.getContent().trim().isEmpty()) {
            throw new IllegalStateException("Agent exceeded max steps (" + max + ") without final answer");
        }

        String answer = result.getContent() != null ? result.getContent() : "";
        String reasoningStored = result.getReasoning();
        if (reasoningStored == null || reasoningStored.trim().isEmpty()) {
            reasoningStored = reasoningAcc.length() > 0 ? reasoningAcc.toString() : null;
        }
        if (answer.trim().isEmpty()) {
            answer = contentAcc.toString();
        }

        List<Map<String, String>> next = new ArrayList<Map<String, String>>(ctx.getMessages());
        next.add(assistantMessage(answer, reasoningStored));
        skillInjectService.stripSkillsFromChatMessages(next);
        conversationStore.putChatMessages(ctx.getConversationId(), next);
        conversationMessageService.appendMessage(
                ctx.getUserId(),
                ctx.getConversationId(),
                SessionType.GENERIC,
                "assistant",
                answer,
                null,
                ctx.getUserMessage(),
                reasoningStored);
    }

    private static List<Map<String, Object>> stringMessagesToObjectMessages(List<Map<String, String>> rows) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, String> row : rows) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("role", row.get("role"));
            String c = row.get("content");
            m.put("content", c != null ? c : "");
            out.add(m);
        }
        return out;
    }

    private static void augmentFirstSystemForWebSearchTool(List<Map<String, Object>> objMsgs) {
        for (int i = 0; i < objMsgs.size(); i++) {
            if ("system".equals(String.valueOf(objMsgs.get(i).get("role")))) {
                Object c = objMsgs.get(i).get("content");
                String base = c != null ? String.valueOf(c) : "";
                objMsgs.get(i).put("content", base + WEB_SEARCH_SYSTEM_APPEND);
                return;
            }
        }
        Map<String, Object> sys = new HashMap<String, Object>();
        sys.put("role", "system");
        sys.put("content", CHAT_SYSTEM_BASE + WEB_SEARCH_SYSTEM_APPEND);
        objMsgs.add(0, sys);
    }

    /**
     * Keeps only {@code role} and {@code content} for OpenAI-compatible APIs (drops stored {@code reasoning}, etc.).
     */
    private static List<Map<String, String>> messagesForLlmApi(List<Map<String, String>> messages) {
        List<Map<String, String>> out = new ArrayList<Map<String, String>>();
        for (Map<String, String> row : messages) {
            Map<String, String> m = new HashMap<String, String>();
            m.put("role", row.get("role"));
            String c = row.get("content");
            m.put("content", c != null ? c : "");
            out.add(m);
        }
        return out;
    }

    @Getter
    public static final class StreamChatContext {
        private final String conversationId;
        private final List<Map<String, String>> messages;
        private final long userId;
        private final String userMessage;
        private final boolean webSearch;
        private final boolean deepThinking;

        public StreamChatContext(String conversationId, List<Map<String, String>> messages, long userId,
                                 String userMessage, boolean webSearch, boolean deepThinking) {
            this.conversationId = conversationId;
            this.messages = messages;
            this.userId = userId;
            this.userMessage = userMessage;
            this.webSearch = webSearch;
            this.deepThinking = deepThinking;
        }

    }

    private static Map<String, String> chatSystem() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("role", "system");
        m.put("content", CHAT_SYSTEM_BASE);
        return m;
    }

    private static Map<String, String> userMessage(String text) {
        Map<String, String> m = new HashMap<String, String>();
        m.put("role", "user");
        m.put("content", text);
        return m;
    }

    private static Map<String, String> assistantMessage(String text, String reasoning) {
        Map<String, String> m = new HashMap<String, String>();
        m.put("role", "assistant");
        m.put("content", text);
        if (reasoning != null && !reasoning.isEmpty()) {
            m.put("reasoning", reasoning);
        }
        return m;
    }

    private List<Map<String, String>> restoreChatMessages(long userId, String conversationId) {
        List<UserConversationMessage> rows = userConversationMessageMapper.selectMessages(
                userId, conversationId, SessionType.GENERIC.name(), 2000);
        if (rows == null || rows.isEmpty()) {
            throw new IllegalStateException("unknown conversationId: " + conversationId);
        }
        List<Map<String, String>> restored = new ArrayList<Map<String, String>>();
        restored.add(chatSystem());
        for (UserConversationMessage row : rows) {
            if ("user".equals(row.getRole())) {
                restored.add(userMessage(row.getContent()));
            } else if ("assistant".equals(row.getRole())) {
                restored.add(assistantMessage(row.getContent(), row.getReasoning()));
            }
        }
        conversationStore.putChatMessages(conversationId, restored);
        return restored;
    }
}
