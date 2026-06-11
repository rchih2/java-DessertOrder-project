package com.stock.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.stock.config.DingTalkConfig;
import com.stock.model.AnomalyEvent;
import com.stock.model.NewsItem;
import com.stock.model.StockSignal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 钉钉企业内部应用推送服务
 * 通过 AppKey + AppSecret 获取 accessToken，使用 AgentId 发送工作通知
 *
 * API文档:
 * - 获取token: https://open.dingtalk.com/document/orgapp/obtain-orgapp-token
 * - 发送工作通知: https://open.dingtalk.com/document/orgapp/asynchronous-sending-of-enterprise-session-messages
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DingTalkService {

    private final DingTalkConfig config;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private volatile String cachedToken;
    private volatile long tokenExpireAt;

    /**
     * 发送文本工作通知
     */
    public boolean sendText(String content) {
        if (!config.isEnabled()) {
            log.debug("钉钉推送未启用");
            return false;
        }
        return sendWorkNotification("text", buildTextPayload(content));
    }

    /**
     * 发送Markdown工作通知
     */
    public boolean sendMarkdown(String title, String markdownContent) {
        if (!config.isEnabled()) {
            log.debug("钉钉推送未启用");
            return false;
        }
        return sendWorkNotification("markdown", buildMarkdownPayload(title, markdownContent));
    }

    /**
     * 推送选股结果
     */
    public boolean sendStockSignals(List<StockSignal> signals) {
        if (signals == null || signals.isEmpty()) return false;

        StringBuilder md = new StringBuilder();
        md.append("### A股智能选股结果\n\n");
        md.append(String.format("> 共发现 **%d** 个信号\n\n", signals.size()));

        for (String period : List.of("monthly", "weekly", "daily")) {
            String periodName = period.equals("monthly") ? "月线" :
                    period.equals("weekly") ? "周线" : "日线";
            List<StockSignal> periodSignals = signals.stream()
                    .filter(s -> period.equals(s.getPeriod()))
                    .toList();
            if (periodSignals.isEmpty()) continue;

            md.append("#### ").append(periodName).append("信号\n\n");

            for (StockSignal signal : periodSignals) {
                String strengthMark = "\u2605".repeat(Math.min(signal.getStrength(), 5));
                md.append(String.format("- **%s(%s)** %s | %s | \u4ef7\u683c:%.2f \u6da8\u8dcc:%.2f%%\n",
                        signal.getName(), signal.getCode(),
                        strengthMark,
                        signal.getDescription(),
                        signal.getCurrentPrice(),
                        signal.getChangePercent()));
            }
            md.append("\n");
        }

        md.append("---\n> \u23f0 ").append(java.time.LocalDateTime.now().format(DT_FMT))
                .append(" | A\u80a1\u667a\u80fd\u9009\u80a1\u7cfb\u7edf");

        return sendMarkdown("\u9009\u80a1\u4fe1\u53f7", md.toString());
    }

    /**
     * 推送异动预警
     */
    public boolean sendAnomalyAlert(List<AnomalyEvent> events) {
        if (events == null || events.isEmpty()) return false;

        StringBuilder md = new StringBuilder();
        md.append("### \u5f02\u52a8\u9884\u8b66\n\n");

        for (AnomalyEvent event : events) {
            if (event == null) continue;
            md.append(String.format("- **%s(%s)** %s\n> \u4ef7\u683c:%.2f \u6da8\u8dcc:%.2f%%\n\n",
                    event.getName(), event.getCode(),
                    event.getDescription(),
                    event.getCurrentPrice(), event.getChangePercent()));
        }

        md.append("---\n> \u23f0 ").append(java.time.LocalDateTime.now().format(DT_FMT));

        return sendMarkdown("\u5f02\u52a8\u9884\u8b66", md.toString());
    }

    /**
     * 推送重要新闻
     */
    public boolean sendNewsDigest(List<NewsItem> newsItems) {
        if (newsItems == null || newsItems.isEmpty()) return false;

        StringBuilder md = new StringBuilder();
        md.append("### \u8d22\u7ecf\u8981\u95fb\u901f\u89c8\n\n");

        int count = 0;
        for (NewsItem item : newsItems) {
            if (count >= 10) break;
            String time = item.getPublishTime() != null ?
                    item.getPublishTime().format(DT_FMT) : "";
            md.append(String.format("- [%s](%s)\n  > %s\n\n",
                    item.getTitle(), item.getUrl(), time));
            count++;
        }

        md.append("---\n> \u23f0 ").append(java.time.LocalDateTime.now().format(DT_FMT));

        return sendMarkdown("\u8d22\u7ecf\u8981\u95fb", md.toString());
    }

    // ========== 私有方法 ==========

    /**
     * 获取钉钉 access_token（带缓存，提前5分钟刷新）
     */
    private String getAccessToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireAt) {
            return cachedToken;
        }

        try {
            String url = String.format(
                    "https://oapi.dingtalk.com/gettoken?appkey=%s&appsecret=%s",
                    config.getAppKey(), config.getAppSecret());

            String response = HttpRequest.get(url)
                    .timeout(10000)
                    .execute()
                    .body();

            JSONObject resp = JSONUtil.parseObj(response);
            int errcode = resp.getInt("errcode", -1);
            if (errcode != 0) {
                log.error("获取钉钉token失败: {}", response);
                return null;
            }

            cachedToken = resp.getStr("access_token");
            int expireIn = resp.getInt("expires_in", 7200);
            tokenExpireAt = System.currentTimeMillis() + (expireIn - 300) * 1000L;

            log.info("钉钉 accessToken 获取成功，有效期{}秒", expireIn);
            return cachedToken;
        } catch (Exception e) {
            log.error("获取钉钉 accessToken 失败", e);
            return null;
        }
    }

    /**
     * 发送企业内部应用工作通知
     * POST https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2
     */
    private boolean sendWorkNotification(String msgType, JSONObject msgContent) {
        try {
            String token = getAccessToken();
            if (token == null) return false;

            JSONObject body = new JSONObject();
            body.set("agent_id", config.getAgentId());
            body.set("msg", buildMessage(msgType, msgContent));

            // 用户列表：配置了就发给指定用户，否则发给全部（user_list 留空）
            String userList = config.getUserIdList();
            body.set("userid_list", (userList != null && !userList.isBlank()) ? userList : "");

            String response = HttpRequest.post(
                            "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2?access_token=" + token)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(10000)
                    .execute()
                    .body();

            log.debug("钉钉响应: {}", response);

            JSONObject resp = JSONUtil.parseObj(response);
            if (resp.getInt("errcode", -1) == 0) {
                return true;
            } else {
                log.warn("钉钉发送失败: {}", response);
                return false;
            }
        } catch (Exception e) {
            log.error("发送钉钉消息失败", e);
            return false;
        }
    }

    /**
     * 构建消息体
     */
    private JSONObject buildMessage(String msgType, JSONObject msgContent) {
        JSONObject msg = new JSONObject();
        msg.set("msgtype", msgType);
        msg.set(msgType, msgContent);
        return msg;
    }

    private JSONObject buildTextPayload(String content) {
        JSONObject payload = new JSONObject();
        payload.set("content", content);
        return payload;
    }

    private JSONObject buildMarkdownPayload(String title, String text) {
        JSONObject payload = new JSONObject();
        payload.set("title", title);
        payload.set("text", text);
        return payload;
    }
}
