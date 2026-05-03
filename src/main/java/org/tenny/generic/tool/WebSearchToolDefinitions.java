package org.tenny.generic.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-style tool definition for on-demand web search ({@link WebSearchQueryTool}).
 */
public final class WebSearchToolDefinitions {

    private WebSearchToolDefinitions() {
    }

    public static List<Map<String, Object>> webSearchToolsDefinition() {
        List<Map<String, Object>> tools = new ArrayList<Map<String, Object>>();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("type", "object");
        Map<String, Object> props = new HashMap<String, Object>();
        Map<String, Object> queryProp = new HashMap<String, Object>();
        queryProp.put("type", "string");
        queryProp.put(
                "description",
                "面向搜索引擎的检索语句，应具体、可检索；不要用于闲聊或无网页需求的问题");
        props.put("query", queryProp);
        parameters.put("properties", props);
        List<String> required = new ArrayList<String>();
        required.add("query");
        parameters.put("required", required);

        Map<String, Object> function = new HashMap<String, Object>();
        function.put("name", WebSearchQueryTool.NAME);
        function.put(
                "description",
                "联网检索网页摘要（Tavily）。仅在需要最新网页事实、新闻、价格、时效、具体实体近况等时调用；"
                        + "用户问候、自我介绍、纯逻辑推导或与网页无关时不要调用。");
        function.put("parameters", parameters);

        Map<String, Object> tool = new HashMap<String, Object>();
        tool.put("type", "function");
        tool.put("function", function);

        tools.add(tool);
        return tools;
    }
}
