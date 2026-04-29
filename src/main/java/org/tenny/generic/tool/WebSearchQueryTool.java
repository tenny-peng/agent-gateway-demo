package org.tenny.generic.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.tenny.generic.service.WebSearchService;

/**
 * Executes {@value #NAME} by delegating to {@link WebSearchService}.
 */
@Component
public class WebSearchQueryTool {

    public static final String NAME = "web_search";

    private final ObjectMapper objectMapper;
    private final WebSearchService webSearchService;

    public WebSearchQueryTool(ObjectMapper objectMapper, WebSearchService webSearchService) {
        this.objectMapper = objectMapper;
        this.webSearchService = webSearchService;
    }

    public String execute(String argumentsJson) {
        String query = "";
        try {
            JsonNode root = objectMapper.readTree(argumentsJson == null ? "{}" : argumentsJson);
            if (root.has("query") && !root.path("query").isNull()) {
                query = root.path("query").asText("").trim();
            }
        } catch (Exception ignored) {
            // keep empty
        }

        WebSearchService.InjectBlock inject = webSearchService.searchAndFormatBlock(query);
        int n = inject.getResultCount();
        String body = inject.getBody();

        StringBuilder sb = new StringBuilder();
        sb.append("【本次检索命中 ").append(n).append(" 条网页摘要】\n");
        sb.append(formatAnswerRules(n));
        sb.append("\n【摘要内容】\n");
        sb.append(body);
        return sb.toString();
    }

    /**
     * Same constraints as former {@code injectWebSearchContext} system block, adapted for tool output.
     */
    private static String formatAnswerRules(int n) {
        String point1 =
                n > 0
                        ? "1) 开头若写「综合了 N 条网页信息」等，其中的 N 必须等于 "
                        + n
                        + "，且必须与文末「---参考来源---」下列出的 URL 行数完全一致（恰好 "
                        + n
                        + " 行，不可多也不可少）。禁止正文写「综合了 "
                        + n
                        + " 条」而参考区只列更少。每一行对应上方摘要中的一条，该行 URL 必须与该条摘要里的 URL 完全一致（可从摘要复制）。"
                        + "若若干条与问题弱相关，在正文说明筛选结论，但参考区仍须把这 "
                        + n
                        + " 条全部列出，简述里可标注「与主题弱相关」等。\n"
                        : "1) 若上方显示未命中网页摘要：开头如实说明，不要虚构条数，不要输出「---参考来源---」参考区块。\n";
        String point4 =
                n > 0
                        ? "4) 参考链接集中收纳：正文结束后空一行输出参考区块；禁止 HTML。"
                        + "第一行且仅为 ---参考来源--- ；自第二行起恰好 "
                        + n
                        + " 行，每行纯文本：完整 http(s) URL + 半角空格 + 一句话简述（不要用 Markdown 链接）。"
                        + "这 "
                        + n
                        + " 行与上方 "
                        + n
                        + " 条摘要一一对应，顺序建议与摘要编号一致。\n"
                        : "4) 无命中时不要输出「---参考来源---」参考区块。\n";
        return "回答时请遵守：\n"
                + point1
                + "2) 先归纳后作答，不要大段照抄摘要原文；遇到列举、推荐、对比、有哪些类问题须分点写多项，"
                + "综合多条摘要里的不同要点，禁止把多项压成单条笼统结论。\n"
                + "3) 正文排版：主体回答里不要出现 http/https 裸链，也不要使用 Markdown 链接 [文字](url)、角标式来源或「见：URL」；"
                + "只用通顺文字陈述，避免每条内容后紧跟一个链接造成版面杂乱。\n"
                + point4
                + "5) 若问题缺少关键条件（如城市/地区/时间）或检索结果相互矛盾，先说明并向用户追问。\n"
                + "6) 无法确认时要明确说不确定。\n";
    }
}
