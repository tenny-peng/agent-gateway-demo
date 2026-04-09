package org.tenny.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.tenny.config.LlmProperties;

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

/**
 * OpenAI-compatible streaming chat (SSE). Uses HttpURLConnection because RestTemplate buffers the body.
 */
@Component
public class LlmStreamClient {

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    public LlmStreamClient(LlmProperties llmProperties, ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Calls chat/completions with stream=true and invokes consumer for each text delta (may be empty chunks; caller can ignore).
     */
    public void streamChatCompletions(List<Map<String, String>> messages, StreamDeltaConsumer onDelta)
            throws IOException {
        String apiKey = LlmKeyUtil.normalizeApiKey(llmProperties.getApiKey());
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("Missing API key: set llm.api-key or environment variable HUNYUAN_API_KEY");
        }

        String urlStr = LlmKeyUtil.trimTrailingSlash(llmProperties.getBaseUrl()) + "/chat/completions";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(llmProperties.getTimeoutMs());
        conn.setReadTimeout(llmProperties.getStreamTimeoutMs());
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        List<Map<String, Object>> msgObj = new ArrayList<Map<String, Object>>();
        for (Map<String, String> row : messages) {
            msgObj.add(new HashMap<String, Object>(row));
        }
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("model", llmProperties.getModel());
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
