package org.tenny.logistics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.tenny.auth.entity.UserConversationMessage;
import org.tenny.auth.mapper.UserConversationMessageMapper;
import org.tenny.auth.model.SessionType;
import org.tenny.auth.service.ConversationMessageService;
import org.tenny.auth.service.ConversationTrackingService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tenny.client.LlmClient;
import org.tenny.client.LlmCompletionResult;
import org.tenny.client.LlmStreamClient;
import org.tenny.client.LlmToolCall;
import org.tenny.common.session.ConversationStore;
import org.tenny.config.AgentProperties;
import org.tenny.config.LlmConfigProvider;
import org.tenny.dto.AgentChatResponse;
import org.tenny.logistics.tool.LogisticsWaybillToolDefinitions;
import org.tenny.logistics.tool.WaybillQueryTool;
import org.tenny.rag.RagService;
import org.tenny.skill.service.SkillInjectService;
import org.tenny.util.JsonLogging;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Logistics business agent: {@code query_waybill} tool loop + optional RAG.
 */
@Service
public class LogisticsAgentService {

    private static final Logger log = LoggerFactory.getLogger(LogisticsAgentService.class);

    private static final String AGENT_SYSTEM_BASE =
            "你是物流助手。用户询问运单、快递、物流状态时，必须调用 query_waybill 工具查询，"
                    + "不要编造轨迹。工具返回后，用简洁中文回复用户。";

    private final LlmClient llmClient;
    private final LlmStreamClient llmStreamClient;
    private final LlmConfigProvider llmConfigProvider;
    private final AgentProperties agentProperties;
    private final WaybillQueryTool waybillQueryTool;
    private final ConversationStore conversationStore;
    private final RagService ragService;
    private final SkillInjectService skillInjectService;
    private final ConversationTrackingService conversationTrackingService;
    private final ConversationMessageService conversationMessageService;
    private final UserConversationMessageMapper userConversationMessageMapper;

    public LogisticsAgentService(LlmClient llmClient,
                                 LlmStreamClient llmStreamClient,
                                 LlmConfigProvider llmConfigProvider,
                                 AgentProperties agentProperties,
                                 WaybillQueryTool waybillQueryTool,
                                 ConversationStore conversationStore,
                                 RagService ragService,
                                 SkillInjectService skillInjectService,
                                 ConversationTrackingService conversationTrackingService,
                                 ConversationMessageService conversationMessageService,
                                 UserConversationMessageMapper userConversationMessageMapper) {
        this.llmClient = llmClient;
        this.llmStreamClient = llmStreamClient;
        this.llmConfigProvider = llmConfigProvider;
        this.agentProperties = agentProperties;
        this.waybillQueryTool = waybillQueryTool;
        this.conversationStore = conversationStore;
        this.ragService = ragService;
        this.skillInjectService = skillInjectService;
        this.conversationTrackingService = conversationTrackingService;
        this.conversationMessageService = conversationMessageService;
        this.userConversationMessageMapper = userConversationMessageMapper;
    }

    public AgentChatResponse run(String userMessage, String conversationId, long userId) {
        long start = System.currentTimeMillis();

        AgentSession session = prepareAgentSession(userMessage, conversationId, userId);
        String convId = session.getConvId();
        List<Map<String, Object>> messages = session.getMessages();

        List<Map<String, Object>> tools = LogisticsWaybillToolDefinitions.waybillToolsDefinition();
        int steps = 0;
        int max = Math.max(1, agentProperties.getMaxSteps());

        while (steps < max) {
            steps++;
            LlmCompletionResult result = llmClient.chatCompletions(messages, tools);

            if (result.hasToolCalls()) {
                log.info("[LogisticsAgent] step {} -> tool_calls: {}", steps, summarizeToolCalls(result.getToolCalls()));
                messages.add(result.getAssistantMessage());
                for (LlmToolCall call : result.getToolCalls()) {
                    String payload = executeTool(call.getName(), call.getArguments());
                    log.info("[LogisticsAgent] tool {} -> {}", call.getName(), JsonLogging.truncate(payload, 500));
                    Map<String, Object> toolMsg = new HashMap<String, Object>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", call.getId());
                    toolMsg.put("content", payload);
                    messages.add(toolMsg);
                    conversationMessageService.appendMessage(
                            userId, convId, SessionType.LOGISTICS, "tool", payload, call.getName(), userMessage);
                }
                continue;
            }

            if (result.getContent() != null && !result.getContent().trim().isEmpty()) {
                log.info("[LogisticsAgent] step {} -> text: {}", steps, JsonLogging.truncate(result.getContent(), 800));
                Map<String, Object> assistant = new HashMap<String, Object>();
                assistant.put("role", "assistant");
                assistant.put("content", result.getContent());
                messages.add(assistant);
                ragService.stripRagFromAgentMessages(messages);
                skillInjectService.stripSkillsFromAgentMessages(messages);
                conversationStore.putAgentMessages(convId, messages);
                conversationMessageService.appendMessage(
                        userId, convId, SessionType.LOGISTICS, "user", userMessage, null, userMessage);
                conversationMessageService.appendMessage(
                        userId, convId, SessionType.LOGISTICS, "assistant", result.getContent(), null, userMessage);

                long latency = System.currentTimeMillis() - start;
                log.info("[LogisticsAgent] done steps={}, latencyMs={}", steps, latency);
                return new AgentChatResponse(result.getContent(), llmConfigProvider.getModel(), latency, steps, convId);
            }

            throw new IllegalStateException("LLM returned empty content and no tool_calls; raw=" + result.getAssistantMessage());
        }

        throw new IllegalStateException("Agent exceeded max steps (" + max + ") without final answer");
    }

    public void runStream(String userMessage, String conversationId, long userId, SseEmitter emitter, AtomicBoolean isCompleted) throws IOException {
        long start = System.currentTimeMillis();
        MediaType textUtf8 = MediaType.parseMediaType("text/plain;charset=UTF-8");
        MediaType jsonUtf8 = MediaType.parseMediaType("application/json;charset=UTF-8");

        AgentSession session = prepareAgentSession(userMessage, conversationId, userId);
        String convId = session.getConvId();
        List<Map<String, Object>> messages = session.getMessages();

        String meta = "{\"conversationId\":\"" + convId + "\"}";
        emitter.send(SseEmitter.event().data(meta, jsonUtf8));

        List<Map<String, Object>> tools = LogisticsWaybillToolDefinitions.waybillToolsDefinition();
        int steps = 0;
        int max = Math.max(1, agentProperties.getMaxSteps());

        while (steps < max) {
            steps++;
            log.info("[LogisticsAgentStream] step {}/{}, conversationId={}", steps, max, convId);

            LlmCompletionResult result = llmStreamClient.streamChatCompletionsWithTools(messages, tools, piece -> {
                if (isCompleted.get()) {
                    throw new RuntimeException("Stream interrupted");
                }
                emitter.send(SseEmitter.event().data(piece, textUtf8));
            });

            if (result.hasToolCalls()) {
                log.info("[LogisticsAgentStream] step {} -> tool_calls: {}", steps, summarizeToolCalls(result.getToolCalls()));
                messages.add(result.getAssistantMessage());
                for (LlmToolCall call : result.getToolCalls()) {
                    log.info("[LogisticsAgentStream] tool_call name={} id={} arguments={}",
                            call.getName(), call.getId(), JsonLogging.truncate(call.getArguments(), 400));
                    String payload = executeTool(call.getName(), call.getArguments());
                    log.info("[LogisticsAgentStream] tool_result name={} content={}", call.getName(), JsonLogging.truncate(payload, 2000));
                    Map<String, Object> toolMsg = new HashMap<String, Object>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", call.getId());
                    toolMsg.put("content", payload);
                    messages.add(toolMsg);
                    conversationMessageService.appendMessage(
                            userId, convId, SessionType.LOGISTICS, "tool", payload, call.getName(), userMessage);
                }
                continue;
            }

            if (result.getContent() != null && !result.getContent().trim().isEmpty()) {
                messages.add(result.getAssistantMessage());
                ragService.stripRagFromAgentMessages(messages);
                skillInjectService.stripSkillsFromAgentMessages(messages);
                conversationStore.putAgentMessages(convId, messages);
                conversationMessageService.appendMessage(
                        userId, convId, SessionType.LOGISTICS, "user", userMessage, null, userMessage);
                conversationMessageService.appendMessage(
                        userId, convId, SessionType.LOGISTICS, "assistant", result.getContent(), null, userMessage);
                long latency = System.currentTimeMillis() - start;
                log.info("[LogisticsAgentStream] done conversationId={} model={} steps={} latencyMs={} answerChars={}",
                        convId, llmConfigProvider.getModel(), steps, latency, result.getContent().length());
                log.debug("[LogisticsAgentStream] answer: {}", JsonLogging.truncate(result.getContent(), 4000));
                emitter.complete();
                return;
            }

            throw new IllegalStateException("LLM returned empty content and no tool_calls (stream)");
        }

        throw new IllegalStateException("Agent exceeded max steps (" + max + ") without final answer");
    }

    private AgentSession prepareAgentSession(String userMessage, String conversationId, long userId) {
        String convId;
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();

        if (conversationId == null || conversationId.trim().isEmpty()) {
            convId = conversationStore.newConversationId();
            conversationTrackingService.recordIfNew(userId, convId, SessionType.LOGISTICS);
            Map<String, Object> system = new HashMap<String, Object>();
            system.put("role", "system");
            system.put("content", AGENT_SYSTEM_BASE);
            messages.add(system);
            Map<String, Object> user = new HashMap<String, Object>();
            user.put("role", "user");
            user.put("content", userMessage);
            messages.add(user);
        } else {
            convId = conversationId.trim();
            List<Map<String, Object>> previous = conversationStore.getAgentMessages(convId);
            if (previous == null) {
                previous = restoreAgentMessages(userId, convId);
            }
            messages.addAll(previous);
            Map<String, Object> user = new HashMap<String, Object>();
            user.put("role", "user");
            user.put("content", userMessage);
            messages.add(user);
        }
        ragService.augmentAgentSystem(messages, userMessage);
        skillInjectService.augmentAgentSystem(messages, userMessage, userId);
        return new AgentSession(convId, messages);
    }

    private String executeTool(String name, String argumentsJson) {
        if (WaybillQueryTool.NAME.equals(name)) {
            return waybillQueryTool.execute(argumentsJson);
        }
        return "{\"error\":\"unknown_tool\"}";
    }

    private static String summarizeToolCalls(List<LlmToolCall> calls) {
        if (calls == null || calls.isEmpty()) {
            return "(none)";
        }
        StringBuilder sb = new StringBuilder();
        for (LlmToolCall c : calls) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(c.getName()).append("(").append(JsonLogging.truncate(c.getArguments(), 200)).append(")");
        }
        return sb.toString();
    }

    private List<Map<String, Object>> restoreAgentMessages(long userId, String conversationId) {
        List<UserConversationMessage> rows = userConversationMessageMapper.selectMessages(
                userId, conversationId, SessionType.LOGISTICS.name(), 2000);
        if (rows == null || rows.isEmpty()) {
            throw new IllegalStateException("unknown conversationId: " + conversationId);
        }
        List<Map<String, Object>> restored = new ArrayList<Map<String, Object>>();
        Map<String, Object> system = new HashMap<String, Object>();
        system.put("role", "system");
        system.put("content", AGENT_SYSTEM_BASE);
        restored.add(system);
        for (UserConversationMessage row : rows) {
            if ("user".equals(row.getRole()) || "assistant".equals(row.getRole())) {
                Map<String, Object> msg = new HashMap<String, Object>();
                msg.put("role", row.getRole());
                msg.put("content", row.getContent());
                restored.add(msg);
            }
        }
        conversationStore.putAgentMessages(conversationId, restored);
        return restored;
    }

    private static final class AgentSession {
        private final String convId;
        private final List<Map<String, Object>> messages;

        AgentSession(String convId, List<Map<String, Object>> messages) {
            this.convId = convId;
            this.messages = messages;
        }

        String getConvId() {
            return convId;
        }

        List<Map<String, Object>> getMessages() {
            return messages;
        }
    }
}
