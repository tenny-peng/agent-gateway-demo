package org.tenny.logistics.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-style tool definitions for the logistics demo agent ({@link WaybillQueryTool}).
 */
public final class LogisticsWaybillToolDefinitions {

    private LogisticsWaybillToolDefinitions() {
    }

    public static List<Map<String, Object>> waybillToolsDefinition() {
        List<Map<String, Object>> tools = new ArrayList<Map<String, Object>>();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("type", "object");
        Map<String, Object> props = new HashMap<String, Object>();
        Map<String, String> waybillProp = new HashMap<String, String>();
        waybillProp.put("type", "string");
        waybillProp.put("description", "运单号，例如 SF1234567890");
        props.put("waybill_no", waybillProp);
        parameters.put("properties", props);
        List<String> required = new ArrayList<String>();
        required.add("waybill_no");
        parameters.put("required", required);

        Map<String, Object> function = new HashMap<String, Object>();
        function.put("name", WaybillQueryTool.NAME);
        function.put("description", "根据运单号查询物流状态（演示接口，返回模拟数据）");
        function.put("parameters", parameters);

        Map<String, Object> tool = new HashMap<String, Object>();
        tool.put("type", "function");
        tool.put("function", function);

        tools.add(tool);
        return tools;
    }
}
