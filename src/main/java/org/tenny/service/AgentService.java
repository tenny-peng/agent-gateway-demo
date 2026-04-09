package org.tenny.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tenny.client.LlmClient;
import org.tenny.client.LlmCompletionResult;
import org.tenny.client.LlmToolCall;
import org.tenny.config.AgentProperties;
import org.tenny.config.LlmProperties;
import org.tenny.dto.AgentChatResponse;
import org.tenny.tool.WaybillQueryTool;
import org.tenny.util.JsonLogging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final LlmClient llmClient;
    private final LlmProperties llmProperties;
    private final AgentProperties agentProperties;
    private final WaybillQueryTool waybillQueryTool;
    private final ConversationStore conversationStore;

    public AgentService(LlmClient llmClient,
                        LlmProperties llmProperties,
                        AgentProperties agentProperties,
                        WaybillQueryTool waybillQueryTool,
                        ConversationStore conversationStore) {
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
        this.agentProperties = agentProperties;
        this.waybillQueryTool = waybillQueryTool;
        this.conversationStore = conversationStore;
    }

    /**
     * Agent loop with optional multi-turn: pass {@code conversationId} from the previous {@link AgentChatResponse}.
     */
    public AgentChatResponse run(String userMessage, String conversationId) {
        long start = System.currentTimeMillis();

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
}
