package org.tenny.common.helper.llmclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.tenny.common.helper.llmclient.dto.LlmCompletionResult;
import org.tenny.common.helper.llmclient.dto.LlmToolCall;
import org.tenny.llmconfig.entity.LlmConfig;
import org.tenny.llmconfig.service.LlmConfigService;
import org.tenny.common.utils.LlmKeyUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LlmClient {

    private final RestTemplate restTemplate;
    private final LlmConfigService llmConfigService;
    private final ObjectMapper objectMapper;

    /**
     * Simple one-shot chat (no tools). Fails if the model returns tool_calls.
     */
    public String chatCompletions(List<Map<String, String>> messages) {
        List<Map<String, Object>> asObject = new ArrayList<Map<String, Object>>();
        for (Map<String, String> row : messages) {
            asObject.add(new HashMap<String, Object>(row));
        }
        LlmCompletionResult result = chatCompletions(asObject, null);
        if (result.hasToolCalls()) {
            throw new IllegalStateException("Model returned tool_calls but simple chat mode does not handle them");
        }
        if (result.getContent() == null || result.getContent().trim().isEmpty()) {
            throw new IllegalStateException("LLM response missing message.content");
        }
        return result.getContent();
    }

    /**
     * Chat completion with optional OpenAI-style tools. Parses content and/or tool_calls.
     */
    public LlmCompletionResult chatCompletions(List<Map<String, Object>> messages,
                                               List<Map<String, Object>> tools) {
        LlmConfig activeConfig = llmConfigService.getActiveConfig();
        String apiKey = LlmKeyUtil.normalizeApiKey(activeConfig.getApiKey());
        if (apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Missing API key: configure LLM config in database or set environment variable API_KEY");
        }
        String url = LlmKeyUtil.trimTrailingSlash(activeConfig.getBaseUrl()) + "/chat/completions";

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("model", activeConfig.getModel());
        body.put("messages", messages);
        body.put("stream", Boolean.FALSE);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("LLM response has no choices: " + response.getBody());
            }
            JsonNode message = choices.get(0).path("message");
            if (message.isMissingNode() || message.isNull()) {
                throw new IllegalStateException("LLM response missing message: " + response.getBody());
            }

            String content = null;
            JsonNode contentNode = message.path("content");
            if (!contentNode.isNull() && !contentNode.isMissingNode()) {
                content = contentNode.asText();
                if (content != null && content.isEmpty()) {
                    content = null;
                }
            }

            List<LlmToolCall> toolCalls = parseToolCalls(message.path("tool_calls"));

            Map<String, Object> assistantMessage = objectMapper.convertValue(
                    message, new TypeReference<Map<String, Object>>() { });

            return new LlmCompletionResult(content, toolCalls, assistantMessage);
        } catch (HttpStatusCodeException e) {
            String detail = e.getResponseBodyAsString();
            if (e.getStatusCode().is4xxClientError()) {
                throw new IllegalStateException("LLM HTTP " + e.getStatusCode() + ": " + detail
                        + " (check API key: no quotes, no 'Bearer ' prefix in env; must be 混元「API Key」而非 SecretId)", e);
            }
            throw new IllegalStateException("LLM HTTP " + e.getStatusCode() + ": " + detail, e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException("LLM call failed: " + e.getMessage(), e);
        }
    }

    private static List<LlmToolCall> parseToolCalls(JsonNode toolCallsNode) {
        List<LlmToolCall> out = new ArrayList<LlmToolCall>();
        if (toolCallsNode == null || !toolCallsNode.isArray()) {
            return out;
        }
        for (JsonNode call : toolCallsNode) {
            String id = call.path("id").asText("");
            JsonNode fn = call.path("function");
            String name = fn.path("name").asText("");
            String args = fn.path("arguments").asText("");
            if (!id.isEmpty() && !name.isEmpty()) {
                out.add(new LlmToolCall(id, name, args));
            }
        }
        return out;
    }

    public static List<Map<String, String>> defaultMessages(String userMessage) {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        Map<String, String> system = new HashMap<String, String>();
        system.put("role", "system");
        system.put("content", "You are a helpful assistant.");
        list.add(system);
        Map<String, String> user = new HashMap<String, String>();
        user.put("role", "user");
        user.put("content", userMessage);
        list.add(user);
        return list;
    }

}
