package org.tenny.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo tool: fake TMS lookup by waybill number (no real HTTP).
 */
@Component
public class WaybillQueryTool {

    public static final String NAME = "query_waybill";

    private final ObjectMapper objectMapper;

    public WaybillQueryTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String execute(String argumentsJson) {
        String waybillNo = "UNKNOWN";
        try {
            JsonNode root = objectMapper.readTree(argumentsJson == null ? "{}" : argumentsJson);
            if (root.has("waybill_no") && !root.path("waybill_no").isNull()) {
                waybillNo = root.path("waybill_no").asText(waybillNo);
            }
        } catch (Exception ignored) {
            // keep UNKNOWN
        }

        Map<String, String> result = new HashMap<String, String>();
        result.put("waybill_no", waybillNo);
        result.put("status", "运输中");
        result.put("last_node", "上海分拨中心");
        result.put("note", "此为演示数据，非真实 TMS");
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\":\"serialize_failed\"}";
        }
    }
}
