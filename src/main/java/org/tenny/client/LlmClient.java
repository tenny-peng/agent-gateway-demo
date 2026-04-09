package org.tenny.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.tenny.config.LlmProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LlmClient {

    private final RestTemplate restTemplate;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    public LlmClient(@Qualifier("llmRestTemplate") RestTemplate restTemplate,
                     LlmProperties llmProperties,
                     ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
    }

    public String chatCompletions(List<Map<String, String>> messages) {
        String apiKey = normalizeApiKey(llmProperties.getApiKey());
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("Missing API key: set llm.api-key or environment variable HUNYUAN_API_KEY");
        }

        String url = trimTrailingSlash(llmProperties.getBaseUrl()) + "/chat/completions";

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("model", llmProperties.getModel());
        body.put("messages", messages);
        body.put("stream", Boolean.FALSE);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                throw new IllegalStateException("LLM response has no choices: " + response.getBody());
            }
            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new IllegalStateException("LLM response missing message.content: " + response.getBody());
            }
            return content.asText();
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

    /**
     * IDEA / 系统环境变量里常见：首尾空格、整段带引号、或误填成 "Bearer sk-xxx"（会与 setBearerAuth 重复）。
     */
    private static String normalizeApiKey(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        if (s.length() >= 2 && s.startsWith("'") && s.endsWith("'")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        if (s.regionMatches(true, 0, "Bearer ", 0, 7)) {
            s = s.substring(7).trim();
        }
        return s;
    }

    private static String trimTrailingSlash(String base) {
        if (base == null) {
            return "";
        }
        String s = base.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
