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
        sb.append("【以下为检索到的网页摘要（供你归纳作答；勿向用户复述本段标题或条数）】\n");
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
                        ? "1) 正文禁止出现「综合了 N 条网页信息」「根据本次/上述检索」「共参考 X 条」等汇报检索条数或过程的套话；应开门见山直接作答，与常见网页对话产品一致。溯源仅以文末「---参考来源---」体现，勿在正文重复说明「用了几条网页」。文末参考区须恰好 "
                        + n
                        + " 行（完整 URL + 半角空格 + 简述），与上方 "
                        + n
                        + " 条摘要一一对应，每行 URL 须与该条摘要中的 URL 完全一致（可从摘要复制）。禁止参考区行数少于此数。若若干条与问题弱相关，可在正文说明筛选结论，但参考区仍须列出全部 "
                        + n
                        + " 条，简述里可标注「与主题弱相关」等。\n"
                        : "1) 若上方显示未命中网页摘要：如实说明即可，不要虚构条数，不要输出「---参考来源---」参考区块。\n";
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
