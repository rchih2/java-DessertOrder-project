package com.stock.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.stock.config.TencentAIConfig;
import com.stock.model.NewsItem;
import com.stock.model.StockSignal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 腾讯混元大模型接入服务
 * 用于: 新闻摘要生成、选股结果智能点评、市场分析等
 *
 * 支持通过OpenAI兼容接口调用腾讯混元
 * 文档: https://cloud.tencent.com/document/product/1729/101844
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TencentAIService {

    private final TencentAIConfig config;

    /**
     * 调用腾讯混元大模型（OpenAI兼容接口）
     *
     * @param prompt 提示词
     * @return 模型回复
     */
    public String chat(String prompt) {
        if (!config.isEnabled()) {
            log.debug("腾讯AI未启用");
            return "";
        }

        try {
            // 腾讯混元 OpenAI 兼容接口
            String url = "https://api.hunyuan.cloud.tencent.com/v1/chat/completions";

            JSONObject body = new JSONObject();
            body.set("model", config.getModel());
            JSONArray messages = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.set("role", "user");
            msg.set("content", prompt);
            messages.add(msg);
            body.set("messages", messages);
            body.set("temperature", 0.7);
            body.set("max_tokens", 2000);

            String response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .body(body.toString())
                    .timeout(30000)
                    .execute()
                    .body();

            JSONObject resp = JSONUtil.parseObj(response);
            if (resp.containsKey("choices")) {
                JSONArray choices = resp.getJSONArray("choices");
                if (!choices.isEmpty()) {
                    return choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getStr("content", "");
                }
            }

            log.warn("腾讯AI响应异常: {}", response);
            return "";

        } catch (Exception e) {
            log.error("调用腾讯AI失败", e);
            return "";
        }
    }

    /**
     * 新闻智能摘要 + 市场影响分析
     */
    public String analyzeNews(List<NewsItem> newsItems) {
        if (newsItems == null || newsItems.isEmpty()) return "";

        String newsText = newsItems.stream()
                .limit(10)
                .map(n -> "- " + n.getTitle() + (n.getSummary() != null ? " " + n.getSummary() : ""))
                .collect(Collectors.joining("\n"));

        String prompt = """
                你是一位资深的A股市场分析师。请根据以下最新财经新闻，进行分析和点评：
                1. 提取2-3条最重要的新闻进行简要解读
                2. 分析这些新闻对A股市场整体的影响（利好/利空/中性）
                3. 给出今日操作建议

                请用简洁专业的语言回答，200字以内。

                今日新闻：
                %s
                """.formatted(newsText);

        return chat(prompt);
    }

    /**
     * 选股结果智能点评
     */
    public String analyzeStockSignals(List<StockSignal> signals) {
        if (signals == null || signals.isEmpty()) return "";

        String signalsText = signals.stream()
                .limit(15)
                .map(s -> String.format("- %s(%s): %s, 价格%.2f, 涨跌%.2f%%, 周期:%s, 强度:%d",
                        s.getName(), s.getCode(), s.getDescription(),
                        s.getCurrentPrice(), s.getChangePercent(),
                        s.getPeriod(), s.getStrength()))
                .collect(Collectors.joining("\n"));

        String prompt = """
                你是一位经验丰富的A股技术分析师。请根据以下选股信号进行点评：
                1. 挑选最值得关注的3-5只股票进行简要分析
                2. 评估各信号的可靠性和风险
                3. 给出操作建议（哪些可以关注，哪些需要观望）

                请用简洁专业的语言回答，300字以内。

                选股信号：
                %s
                """.formatted(signalsText);

        return chat(prompt);
    }

    /**
     * 个股智能分析
     */
    public String analyzeSingleStock(String code, String name, double price,
                                      double changePercent, String klineSummary) {
        String prompt = """
                你是一位资深A股分析师。请对以下股票进行简要分析：

                股票：%s(%s)
                当前价格：%.2f元
                今日涨跌幅：%.2f%%

                技术面概要：
                %s

                请从以下角度分析（150字以内）：
                1. 技术面趋势判断
                2. 关键支撑/压力位
                3. 操作建议
                """.formatted(name, code, price, changePercent, klineSummary);

        return chat(prompt);
    }
}
