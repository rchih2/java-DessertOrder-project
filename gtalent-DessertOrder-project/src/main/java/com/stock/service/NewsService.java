package com.stock.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stock.model.NewsItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 财经新闻服务
 * 使用东方财富/新浪财经等免费API获取新闻
 */
@Slf4j
@Service
public class NewsService {

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取最新财经新闻（宏观+行业）
     * 使用东方财富API
     */
    public List<NewsItem> getLatestFinancialNews(int count) {
        String url = String.format(
                "https://np-listapi.eastmoney.com/comm/web/getNewsByColumns?client=web&biz=web_news_col&column=350&order=1&needInteractData=0&page_index=1&page_size=%d",
                count);

        try {
            String response = webClient.get()
                    .uri(url)
                    .header("Referer", "https://finance.eastmoney.com")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseEastMoneyNews(response, "macro");
        } catch (Exception e) {
            log.error("获取财经新闻失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取股市要闻
     */
    public List<NewsItem> getStockMarketNews(int count) {
        String url = String.format(
                "https://np-listapi.eastmoney.com/comm/web/getNewsByColumns?client=web&biz=web_news_col&column=162&order=1&needInteractData=0&page_index=1&page_size=%d",
                count);

        try {
            String response = webClient.get()
                    .uri(url)
                    .header("Referer", "https://finance.eastmoney.com")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseEastMoneyNews(response, "stock");
        } catch (Exception e) {
            log.error("获取股市要闻失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取新浪财经7x24快讯
     */
    public List<NewsItem> getSinaFlashNews(int count) {
        String url = String.format(
                "https://feed.mix.sina.com.cn/api/roll/get?pageid=153&lid=2516&k=&num=%d&page=1&r=0.0",
                count);

        try {
            String response = webClient.get()
                    .uri(url)
                    .header("Referer", "https://finance.sina.com.cn")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseSinaNews(response);
        } catch (Exception e) {
            log.error("获取新浪快讯失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 汇总所有新闻源
     */
    public List<NewsItem> getAllImportantNews(int count) {
        List<NewsItem> allNews = new ArrayList<>();

        List<NewsItem> financial = getLatestFinancialNews(count);
        allNews.addAll(financial);

        List<NewsItem> market = getStockMarketNews(count);
        allNews.addAll(market);

        List<NewsItem> flash = getSinaFlashNews(count);
        allNews.addAll(flash);

        // 按时间倒序
        allNews.sort((a, b) -> {
            if (a.getPublishTime() == null || b.getPublishTime() == null) return 0;
            return b.getPublishTime().compareTo(a.getPublishTime());
        });

        // 去重（按标题）
        List<NewsItem> unique = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (NewsItem item : allNews) {
            if (seen.add(item.getTitle())) {
                unique.add(item);
            }
        }

        return unique.stream().limit(count).toList();
    }

    // ========== 解析方法 ==========

    private List<NewsItem> parseEastMoneyNews(String response, String category) {
        List<NewsItem> result = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data");
            if (data == null) return result;

            JsonArray list = data.getAsJsonArray("list");
            if (list == null) return result;

            for (JsonElement element : list) {
                JsonObject item = element.getAsJsonObject();
                String title = getStr(item, "title");
                String summary = getStr(item, "digest") != null ? getStr(item, "digest") :
                        getStr(item, "description");
                String url = getStr(item, "url");
                String source = getStr(item, "source");
                String showTime = getStr(item, "showTime");

                LocalDateTime publishTime = null;
                try {
                    if (showTime != null) {
                        publishTime = LocalDateTime.parse(showTime, DT_FMT);
                    }
                } catch (Exception ignored) {}

                result.add(NewsItem.builder()
                        .title(title)
                        .summary(summary != null && summary.length() > 100 ?
                                summary.substring(0, 100) + "..." : summary)
                        .source(source)
                        .url(url)
                        .publishTime(publishTime)
                        .category(category)
                        .build());
            }
        } catch (Exception e) {
            log.error("解析东方财富新闻失败", e);
        }
        return result;
    }

    private List<NewsItem> parseSinaNews(String response) {
        List<NewsItem> result = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();
            JsonObject resultObj = root.getAsJsonObject("result");
            if (resultObj == null) return result;

            JsonArray list = resultObj.getAsJsonArray("data");
            if (list == null) return result;

            for (JsonElement element : list) {
                JsonObject item = element.getAsJsonObject();
                String title = getStr(item, "title");
                String url = getStr(item, "url");
                String media = getStr(item, "media");
                String timeStr = getStr(item, "ctime");

                LocalDateTime publishTime = null;
                try {
                    if (timeStr != null) {
                        publishTime = LocalDateTime.parse(timeStr, DT_FMT);
                    }
                } catch (Exception ignored) {}

                result.add(NewsItem.builder()
                        .title(title)
                        .summary(getStr(item, "intro"))
                        .source(media)
                        .url(url)
                        .publishTime(publishTime)
                        .category("flash")
                        .build());
            }
        } catch (Exception e) {
            log.error("解析新浪新闻失败", e);
        }
        return result;
    }

    private String getStr(JsonObject obj, String key) {
        try {
            JsonElement el = obj.get(key);
            return el != null && !el.isJsonNull() ? el.getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
