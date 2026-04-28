package org.tenny.generic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.tenny.common.config.AppProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Internet search for generic chat augmentation via <a href="https://tavily.com">Tavily</a>.
 */
@Service
public class WebSearchService {

    private static final String TAVILY_URL = "https://api.tavily.com/search";

    private final RestTemplate webSearchRestTemplate;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public WebSearchService(
            @Qualifier("webSearchRestTemplate") RestTemplate webSearchRestTemplate,
            AppProperties appProperties,
            ObjectMapper objectMapper) {
        this.webSearchRestTemplate = webSearchRestTemplate;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Runs search and returns a plain-text block suitable for an extra {@code user} message.
     *
     * @throws IllegalStateException if API key is missing or the HTTP call fails
     */
    public String searchAndFormat(String query) {
        String apiKey = trimToEmpty(appProperties.getWebSearch().getApiKey());
        if (apiKey.isEmpty()) {
            throw new IllegalStateException(
                    "联网搜索未配置：请在 application.yml 设置 app.web-search.api-key 或环境变量 TAVILY_API_KEY（https://tavily.com）");
        }
        if (query == null || query.trim().isEmpty()) {
            return "(无检索关键词)";
        }

        AppProperties.WebSearch cfg = appProperties.getWebSearch();
        int maxResults = Math.max(1, Math.min(10, cfg.getMaxResults()));
        int cap = Math.max(200, cfg.getMaxSnippetCharsPerResult());

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("api_key", apiKey);
        body.put("query", query.trim());
        body.put("search_depth", "basic");
        body.put("include_answer", false);
        body.put("max_results", maxResults);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(body, headers);

        ResponseEntity<String> response;
        try {
            response = webSearchRestTemplate.postForEntity(TAVILY_URL, entity, String.class);
        } catch (Exception e) {
            throw new IllegalStateException("联网检索失败: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("联网检索失败: HTTP " + response.getStatusCodeValue());
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode results = root.get("results");
            if (results == null || !results.isArray() || results.isEmpty()) {
                return "（未找到相关网页摘要，请根据你的知识谨慎回答并说明可能无法联网核实。）";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                JsonNode r = results.get(i);
                String title = text(r.get("title"));
                String url = text(r.get("url"));
                String content = text(r.get("content"));
                if (content.length() > cap) {
                    content = content.substring(0, cap) + "…";
                }
                sb.append(i + 1).append(". ").append(title).append("\n");
                if (!url.isEmpty()) {
                    sb.append("   URL: ").append(url).append("\n");
                }
                if (!content.isEmpty()) {
                    sb.append("   ").append(content.replace("\n", " ")).append("\n");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            throw new IllegalStateException("解析检索结果失败: " + e.getMessage(), e);
        }
    }

    private static String text(JsonNode n) {
        if (n == null || n.isNull()) {
            return "";
        }
        return n.asText("").trim();
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
