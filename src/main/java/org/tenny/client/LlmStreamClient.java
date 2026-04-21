package org.tenny.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.tenny.config.LlmConfigProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * OpenAI-compatible streaming chat (SSE). Uses HttpURLConnection because RestTemplate buffers the body.
 */
@Component
public class LlmStreamClient {

    private final LlmConfigProvider llmConfigProvider;
    private final ObjectMapper objectMapper;

    public LlmStreamClient(LlmConfigProvider llmConfigProvider, ObjectMapper objectMapper) {
        this.llmConfigProvider = llmConfigProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * Calls chat/completions with stream=true and invokes consumer for each text delta (may be empty chunks; caller can ignore).
     */
    public void streamChatCompletions(List<Map<String, String>> messages, StreamDeltaConsumer onDelta)
            throws IOException {
        String apiKey = LlmKeyUtil.normalizeApiKey(llmConfigProvider.getApiKey());
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("Missing API key: configure LLM config in database or set environment variable API_KEY");
        }

        String urlStr = LlmKeyUtil.trimTrailingSlash(llmConfigProvider.getBaseUrl()) + "/chat/completions";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(llmConfigProvider.getTimeoutMs());
        conn.setReadTimeout(llmConfigProvider.getStreamTimeoutMs());
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        List<Map<String, Object>> msgObj = new ArrayList<Map<String, Object>>();
        for (Map<String, String> row : messages) {
            msgObj.add(new HashMap<String, Object>(row));
        }
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("model", llmConfigProvider.getModel());
        body.put("messages", msgObj);
        body.put("stream", Boolean.TRUE);

        byte[] json = objectMapper.writeValueAsBytes(body);
        OutputStream os = conn.getOutputStream();
        os.write(json);
        os.close();

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            String err = readStreamAsString(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
            throw new IllegalStateException("LLM HTTP " + code + ": " + err);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring("data:".length()).trim();
                if (payload.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(payload)) {
                    break;
                }
                JsonNode root = objectMapper.readTree(payload);
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.size() == 0) {
                    continue;
                }
                JsonNode delta = choices.get(0).path("delta");
                if (delta.isMissingNode() || delta.isNull()) {
                    continue;
                }
                JsonNode contentNode = delta.path("content");
                if (contentNode.isMissingNode() || contentNode.isNull()) {
                    continue;
                }
                String piece = contentNode.asText("");
                if (!piece.isEmpty()) {
                    onDelta.onDelta(piece);
                }
            }
        } finally {
            reader.close();
            conn.disconnect();
        }
    }

    /**
     * Streaming chat with OpenAI-style tools: merges {@code delta.content} and incremental {@code delta.tool_calls}
     * until {@code [DONE]}. Invokes {@code onContentDelta} for each non-empty text chunk (final answer only; tool rounds
     * usually have no content).
     *
     * @return same shape as {@link LlmClient#chatCompletions(List, List)} for appending to {@code messages}
     */
    public LlmCompletionResult streamChatCompletionsWithTools(List<Map<String, Object>> messages,
                                                                List<Map<String, Object>> tools,
                                                                StreamDeltaConsumer onContentDelta)
            throws IOException {
        String apiKey = LlmKeyUtil.normalizeApiKey(llmConfigProvider.getApiKey());
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("Missing API key: configure LLM config in database or set environment variable API_KEY");
        }

        String urlStr = LlmKeyUtil.trimTrailingSlash(llmConfigProvider.getBaseUrl()) + "/chat/completions";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(llmConfigProvider.getTimeoutMs());
        conn.setReadTimeout(llmConfigProvider.getStreamTimeoutMs());
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("model", llmConfigProvider.getModel());
        body.put("messages", messages);
        body.put("stream", Boolean.TRUE);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        byte[] json = objectMapper.writeValueAsBytes(body);
        OutputStream os = conn.getOutputStream();
        os.write(json);
        os.close();

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            String err = readStreamAsString(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
            throw new IllegalStateException("LLM HTTP " + code + ": " + err);
        }

        StringBuilder fullContent = new StringBuilder();
        Map<Integer, ToolCallAccumulator> toolAcc = new TreeMap<Integer, ToolCallAccumulator>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring("data:".length()).trim();
                if (payload.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(payload)) {
                    break;
                }
                JsonNode root = objectMapper.readTree(payload);
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.size() == 0) {
                    continue;
                }
                JsonNode delta = choices.get(0).path("delta");
                if (delta.isMissingNode() || delta.isNull()) {
                    continue;
                }

                JsonNode contentNode = delta.path("content");
                if (!contentNode.isMissingNode() && !contentNode.isNull()) {
                    String piece = contentNode.asText("");
                    if (!piece.isEmpty()) {
                        fullContent.append(piece);
                        if (onContentDelta != null) {
                            onContentDelta.onDelta(piece);
                        }
                    }
                }

                JsonNode toolCallsDelta = delta.path("tool_calls");
                if (toolCallsDelta.isArray()) {
                    for (JsonNode tc : toolCallsDelta) {
                        mergeToolCallDelta(tc, toolAcc);
                    }
                }
            }
        } finally {
            reader.close();
            conn.disconnect();
        }

        List<LlmToolCall> toolCalls = finishToolCalls(toolAcc);
        String text = fullContent.toString();
        if (!toolCalls.isEmpty()) {
            String contentForMsg = text.isEmpty() ? null : text;
            Map<String, Object> assistantMessage = buildAssistantMessage(contentForMsg, toolCalls);
            return new LlmCompletionResult(null, toolCalls, assistantMessage);
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalStateException("LLM stream ended with empty content and no tool_calls");
        }
        Map<String, Object> assistantMessage = buildAssistantMessage(text, toolCalls);
        return new LlmCompletionResult(text, toolCalls, assistantMessage);
    }

    private static void mergeToolCallDelta(JsonNode tc, Map<Integer, ToolCallAccumulator> toolAcc) {
        int idx = tc.has("index") ? tc.path("index").asInt(0) : 0;
        ToolCallAccumulator acc = toolAcc.get(Integer.valueOf(idx));
        if (acc == null) {
            acc = new ToolCallAccumulator();
            toolAcc.put(Integer.valueOf(idx), acc);
        }
        if (tc.has("id")) {
            String id = tc.path("id").asText("");
            if (!id.isEmpty()) {
                acc.id = id;
            }
        }
        JsonNode fn = tc.path("function");
        if (fn.has("name")) {
            String n = fn.path("name").asText("");
            if (!n.isEmpty()) {
                acc.name = n;
            }
        }
        if (fn.has("arguments")) {
            acc.arguments.append(fn.path("arguments").asText(""));
        }
    }

    private static List<LlmToolCall> finishToolCalls(Map<Integer, ToolCallAccumulator> toolAcc) {
        List<LlmToolCall> out = new ArrayList<LlmToolCall>();
        if (toolAcc.isEmpty()) {
            return out;
        }
        for (Map.Entry<Integer, ToolCallAccumulator> e : toolAcc.entrySet()) {
            ToolCallAccumulator acc = e.getValue();
            if (acc.id == null || acc.id.isEmpty() || acc.name == null || acc.name.isEmpty()) {
                throw new IllegalStateException("Incomplete streamed tool_call at index " + e.getKey());
            }
            out.add(new LlmToolCall(acc.id, acc.name, acc.arguments.toString()));
        }
        return out;
    }

    private Map<String, Object> buildAssistantMessage(String contentOrNull, List<LlmToolCall> toolCalls) {
        Map<String, Object> assistant = new HashMap<String, Object>();
        assistant.put("role", "assistant");
        if (toolCalls != null && !toolCalls.isEmpty()) {
            assistant.put("content", contentOrNull);
            List<Map<String, Object>> tls = new ArrayList<Map<String, Object>>();
            for (LlmToolCall c : toolCalls) {
                Map<String, Object> callMap = new HashMap<String, Object>();
                callMap.put("id", c.getId());
                callMap.put("type", "function");
                Map<String, Object> fn = new HashMap<String, Object>();
                fn.put("name", c.getName());
                fn.put("arguments", c.getArguments());
                callMap.put("function", fn);
                tls.add(callMap);
            }
            assistant.put("tool_calls", tls);
        } else {
            assistant.put("content", contentOrNull);
        }
        return assistant;
    }

    private static final class ToolCallAccumulator {
        private String id;
        private String name;
        private final StringBuilder arguments = new StringBuilder();
    }

    private static String readStreamAsString(java.io.InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String l;
        while ((l = r.readLine()) != null) {
            sb.append(l);
        }
        r.close();
        return sb.toString();
    }

    @FunctionalInterface
    public interface StreamDeltaConsumer {
        void onDelta(String text) throws IOException;
    }
}
