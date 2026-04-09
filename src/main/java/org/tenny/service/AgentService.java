package org.tenny.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tenny.client.LlmClient;
import org.tenny.client.LlmCompletionResult;
import org.tenny.client.LlmStreamClient;
import org.tenny.client.LlmToolCall;
import org.tenny.config.AgentProperties;
import org.tenny.config.LlmProperties;
import org.tenny.dto.AgentChatResponse;
import org.tenny.tool.WaybillQueryTool;
import org.tenny.util.JsonLogging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final LlmClient llmClient;
    private final LlmStreamClient llmStreamClient;
    private final LlmProperties llmProperties;
    private final AgentProperties agentProperties;
    private final WaybillQueryTool waybillQueryTool;
    private final ConversationStore conversationStore;
    private final ObjectMapper objectMapper;

    public AgentService(LlmClient llmClient,
                        LlmStreamClient llmStreamClient,
                        LlmProperties llmProperties,
                        AgentProperties agentProperties,
                        WaybillQueryTool waybillQueryTool,
                        ConversationStore conversationStore,
                        ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.llmStreamClient = llmStreamClient;
        this.llmProperties = llmProperties;
        this.agentProperties = agentProperties;
        this.waybillQueryTool = waybillQueryTool;
        this.conversationStore = conversationStore;
        this.objectMapper = objectMapper;
    }

    /**
     * Agent loop with optional multi-turn: pass {@code conversationId} from the previous {@link AgentChatResponse}.
     */
    public AgentChatResponse run(String userMessage, String conversationId) {
        long start = System.currentTimeMillis();

        AgentSession session = prepareAgentSession(userMessage, conversationId);
        String convId = session.getConvId();
        List<Map<String, Object>> messages = session.getMessages();

        List<Map<String, Object>> tools = LlmClient.waybillToolsDefinition();
        int steps = 0;
        int max = Math.max(1, agentProperties.getMaxSteps());

        while (steps < max) {
            steps++;
            LlmCompletionResult result = llmClient.chatCompletions(messages, tools);

            if (result.hasToolCalls()) {
                log.info("[Agent] 第{}次调模型 → 返回 tool_calls: {}", steps, summarizeToolCalls(result.getToolCalls()));
                messages.add(result.getAssistantMessage());
                for (LlmToolCall call : result.getToolCalls()) {
                    String payload = executeTool(call.getName(), call.getArguments());
                    log.info("[Agent] 工具 {} 返回 → {}", call.getName(), JsonLogging.truncate(payload, 500));
                    Map<String, Object> toolMsg = new HashMap<String, Object>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", call.getId());
                    toolMsg.put("content", payload);
                    messages.add(toolMsg);
                }
                continue;
            }

            if (result.getContent() != null && !result.getContent().trim().isEmpty()) {
                log.info("[Agent] 第{}次调模型 → 返回正文: {}", steps, JsonLogging.truncate(result.getContent(), 800));
                Map<String, Object> assistant = new HashMap<String, Object>();
                assistant.put("role", "assistant");
                assistant.put("content", result.getContent());
                messages.add(assistant);
                conversationStore.putAgentMessages(convId, messages);

                long latency = System.currentTimeMillis() - start;
                log.info("[Agent] 结束 steps={}, latencyMs={}", steps, latency);
                return new AgentChatResponse(result.getContent(), llmProperties.getModel(), latency, steps, convId);
            }

            throw new IllegalStateException("LLM returned empty content and no tool_calls; raw=" + result.getAssistantMessage());
        }

        throw new IllegalStateException("Agent exceeded max steps (" + max + ") without final answer");
    }

    /**
     * SSE agent loop: stream=true with tools; {@code delta} events for final assistant text; {@code tool_call} /
     * {@code tool_result} between rounds. First data line is JSON {@code conversationId} (same convention as
     * {@code /api/chat/stream}).
     */
    public void runStream(String userMessage, String conversationId, SseEmitter emitter) throws IOException {
        long start = System.currentTimeMillis();
        MediaType textUtf8 = MediaType.parseMediaType("text/plain;charset=UTF-8");
        MediaType jsonUtf8 = MediaType.parseMediaType("application/json;charset=UTF-8");

        AgentSession session = prepareAgentSession(userMessage, conversationId);
        String convId = session.getConvId();
        List<Map<String, Object>> messages = session.getMessages();

        String meta = "{\"conversationId\":\"" + convId + "\"}";
        emitter.send(SseEmitter.event().data(meta, jsonUtf8));

        List<Map<String, Object>> tools = LlmClient.waybillToolsDefinition();
        int steps = 0;
        int max = Math.max(1, agentProperties.getMaxSteps());

        while (steps < max) {
            steps++;
            Map<String, Object> stepPayload = new HashMap<String, Object>();
            stepPayload.put("step", Integer.valueOf(steps));
            stepPayload.put("maxSteps", Integer.valueOf(max));
            emitter.send(SseEmitter.event().name("step").data(objectMapper.writeValueAsString(stepPayload), jsonUtf8));

            LlmCompletionResult result = llmStreamClient.streamChatCompletionsWithTools(messages, tools, piece ->
                    emitter.send(SseEmitter.event().name("delta").data(piece, textUtf8)));

            if (result.hasToolCalls()) {
                log.info("[AgentStream] step {} → tool_calls: {}", Integer.valueOf(steps), summarizeToolCalls(result.getToolCalls()));
                messages.add(result.getAssistantMessage());
                for (LlmToolCall call : result.getToolCalls()) {
                    Map<String, Object> tc = new HashMap<String, Object>();
                    tc.put("name", call.getName());
                    tc.put("arguments", call.getArguments());
                    emitter.send(SseEmitter.event().name("tool_call").data(objectMapper.writeValueAsString(tc), jsonUtf8));
                    String payload = executeTool(call.getName(), call.getArguments());
                    log.info("[AgentStream] tool {} → {}", call.getName(), JsonLogging.truncate(payload, 500));
                    emitter.send(SseEmitter.event().name("tool_result").data(
                            objectMapper.writeValueAsString(toolResultPayload(call.getName(), payload)), jsonUtf8));
                    Map<String, Object> toolMsg = new HashMap<String, Object>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", call.getId());
                    toolMsg.put("content", payload);
                    messages.add(toolMsg);
                }
                continue;
            }

            if (result.getContent() != null && !result.getContent().trim().isEmpty()) {
                log.info("[AgentStream] step {} → final text (streamed)", Integer.valueOf(steps));
                messages.add(result.getAssistantMessage());
                conversationStore.putAgentMessages(convId, messages);
                long latency = System.currentTimeMillis() - start;
                log.info("[AgentStream] done steps={}, latencyMs={}", Integer.valueOf(steps), Long.valueOf(latency));
                Map<String, Object> done = new HashMap<String, Object>();
                done.put("latencyMs", Long.valueOf(latency));
                done.put("stepsUsed", Integer.valueOf(steps));
                done.put("conversationId", convId);
                done.put("model", llmProperties.getModel());
                done.put("answer", result.getContent());
                emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(done), jsonUtf8));
                emitter.complete();
                return;
            }

            throw new IllegalStateException("LLM returned empty content and no tool_calls (stream)");
        }

        throw new IllegalStateException("Agent exceeded max steps (" + max + ") without final answer");
    }

    private static Map<String, Object> toolResultPayload(String name, String payload) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("name", name);
        m.put("content", JsonLogging.truncate(payload, 2000));
        return m;
    }

    private AgentSession prepareAgentSession(String userMessage, String conversationId) {
        String convId;
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();

        if (conversationId == null || conversationId.trim().isEmpty()) {
            convId = conversationStore.newConversationId();
            Map<String, Object> system = new HashMap<String, Object>();
            system.put("role", "system");
            system.put("content",
                    "你是物流助手。用户询问运单、快递、物流状态时，必须调用 query_waybill 工具查询，"
                            + "不要编造轨迹。工具返回后，用简洁中文回复用户。");
            messages.add(system);
            Map<String, Object> user = new HashMap<String, Object>();
            user.put("role", "user");
            user.put("content", userMessage);
            messages.add(user);
        } else {
            convId = conversationId.trim();
            List<Map<String, Object>> previous = conversationStore.getAgentMessages(convId);
            if (previous == null) {
                throw new IllegalStateException("unknown conversationId: " + convId);
            }
            messages.addAll(previous);
            Map<String, Object> user = new HashMap<String, Object>();
            user.put("role", "user");
            user.put("content", userMessage);
            messages.add(user);
        }
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
