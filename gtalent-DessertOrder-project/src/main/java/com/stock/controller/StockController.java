package com.stock.controller;

import com.stock.model.*;
import com.stock.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API 控制器
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StockController {

    private final StockDataService stockDataService;
    private final IndicatorService indicatorService;
    private final StockSelectService stockSelectService;
    private final AnomalyDetectService anomalyDetectService;
    private final NewsService newsService;
    private final DingTalkService dingTalkService;
    private final TencentAIService tencentAIService;

    // ========== 股票数据 ==========

    /** 获取股票池 */
    @GetMapping("/pool")
    public Map<String, Object> getStockPool() {
        List<String> pool = stockDataService.loadStockPool();
        return ok("stockPool", pool, pool.size());
    }

    /** 获取实时行情 */
    @GetMapping("/quote/{code}")
    public Map<String, Object> getQuote(@PathVariable String code) {
        return stockDataService.getRealTimeQuote(code)
                .map(q -> ok("quote", q))
                .orElse(fail("获取行情失败"));
    }

    /** 批量获取实时行情 */
    @PostMapping("/quotes")
    public Map<String, Object> getQuotes(@RequestBody List<String> codes) {
        List<StockQuote> quotes = stockDataService.getRealTimeQuotes(codes);
        return ok("quotes", quotes, quotes.size());
    }

    /** 获取K线数据 */
    @GetMapping("/kline/{code}")
    public Map<String, Object> getKLine(
            @PathVariable String code,
            @RequestParam(defaultValue = "daily") String period) {
        List<KLineData> data = stockDataService.getKLineData(code, period);
        return ok("kline", data, data.size());
    }

    // ========== 技术指标 ==========

    /** 计算MACD */
    @GetMapping("/indicator/macd/{code}")
    public Map<String, Object> getMACD(
            @PathVariable String code,
            @RequestParam(defaultValue = "daily") String period) {
        List<KLineData> kline = stockDataService.getKLineData(code, period);
        if (kline.isEmpty()) return fail("K线数据为空");

        List<MACDResult> macd = indicatorService.calculateMACD(kline);
        return ok("macd", macd, macd.size());
    }

    /** 获取全部指标 */
    @GetMapping("/indicator/all/{code}")
    public Map<String, Object> getAllIndicators(
            @PathVariable String code,
            @RequestParam(defaultValue = "daily") String period) {
        List<KLineData> kline = stockDataService.getKLineData(code, period);
        if (kline.isEmpty()) return fail("K线数据为空");

        Map<String, Object> data = new HashMap<>();
        data.put("macd", indicatorService.calculateMACD(kline));
        data.put("ma5", indicatorService.calculateMA(kline, 5));
        data.put("ma10", indicatorService.calculateMA(kline, 10));
        data.put("ma20", indicatorService.calculateMA(kline, 20));
        data.put("ma60", indicatorService.calculateMA(kline, 60));
        data.put("rsi6", indicatorService.calculateRSI(kline, 6));
        data.put("rsi12", indicatorService.calculateRSI(kline, 12));
        data.put("rsi24", indicatorService.calculateRSI(kline, 24));
        data.put("kdj", indicatorService.calculateKDJ(kline, 9));
        data.put("volumeRatio", indicatorService.calculateVolumeRatio(kline, 20));
        data.put("bullishMA", indicatorService.isBullishMALayout(kline));
        data.put("success", true);
        return data;
    }

    // ========== 选股 ==========

    /** 执行选股 */
    @GetMapping("/select")
    public Map<String, Object> selectStocks(
            @RequestParam(defaultValue = "daily,weekly,monthly") List<String> periods) {
        List<StockSignal> signals = stockSelectService.executeStockSelection(periods);
        return ok("signals", signals, signals.size());
    }

    /** 执行选股并推送钉钉 */
    @PostMapping("/select/notify")
    public Map<String, Object> selectAndNotify(
            @RequestParam(defaultValue = "daily,weekly,monthly") List<String> periods) {
        List<StockSignal> signals = stockSelectService.executeStockSelection(periods);

        boolean sent = false;
        if (!signals.isEmpty()) {
            dingTalkService.sendStockSignals(signals);
            sent = true;
        }

        Map<String, Object> result = ok("signals", signals, signals.size());
        result.put("dingTalkSent", sent);
        return result;
    }

    // ========== 异动监测 ==========

    /** 扫描异动 */
    @GetMapping("/anomaly/scan")
    public Map<String, Object> scanAnomalies() {
        List<AnomalyEvent> events = anomalyDetectService.scanAnomalies();
        return ok("anomalies", events, events.size());
    }

    // ========== 新闻 ==========

    /** 获取最新新闻 */
    @GetMapping("/news")
    public Map<String, Object> getNews(@RequestParam(defaultValue = "10") int count) {
        List<NewsItem> news = newsService.getAllImportantNews(count);
        return ok("news", news, news.size());
    }

    // ========== AI分析 ==========

    /** AI分析选股结果 */
    @PostMapping("/ai/analyze-signals")
    public Map<String, Object> aiAnalyzeSignals(@RequestBody List<StockSignal> signals) {
        String analysis = tencentAIService.analyzeStockSignals(signals);
        return ok("analysis", analysis);
    }

    /** AI分析新闻 */
    @PostMapping("/ai/analyze-news")
    public Map<String, Object> aiAnalyzeNews(@RequestBody List<NewsItem> news) {
        String analysis = tencentAIService.analyzeNews(news);
        return ok("analysis", analysis);
    }

    /** AI分析个股 */
    @GetMapping("/ai/analyze/{code}")
    public Map<String, Object> aiAnalyzeStock(@PathVariable String code) {
        var quote = stockDataService.getRealTimeQuote(code);
        if (quote.isEmpty()) return fail("获取行情失败");

        StockQuote q = quote.get();
        List<KLineData> kline = stockDataService.getKLineData(code, "daily");
        List<MACDResult> macd = indicatorService.calculateMACD(kline);

        String summary = String.format("MACD: DIF=%.4f, DEA=%.4f, 均线多头=%b",
                macd.isEmpty() ? 0 : macd.get(macd.size() - 1).getDif(),
                macd.isEmpty() ? 0 : macd.get(macd.size() - 1).getDea(),
                indicatorService.isBullishMALayout(kline));

        String analysis = tencentAIService.analyzeSingleStock(
                code, q.getName(), q.getCurrentPrice(), q.getChangePercent(), summary);

        return ok("analysis", analysis);
    }

    // ========== 钉钉测试 ==========

    /** 测试钉钉推送 */
    @GetMapping("/test/dingtalk")
    public Map<String, Object> testDingTalk() {
        boolean result = dingTalkService.sendMarkdown(
                "测试消息",
                "### 测试消息\n\nA股智能选股系统钉钉推送测试成功！\n\n" +
                        "> 时间: " + java.time.LocalDateTime.now());
        return ok("sent", result);
    }

    // ========== 工具方法 ==========

    private Map<String, Object> ok(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put(key, value);
        return map;
    }

    private Map<String, Object> ok(String key, Object value, int count) {
        Map<String, Object> map = ok(key, value);
        map.put("count", count);
        return map;
    }

    private Map<String, Object> fail(String msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("message", msg);
        return map;
    }
}
