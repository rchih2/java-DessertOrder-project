package com.stock.service;

import com.stock.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 个股异动监测服务
 * 监测: 急涨急跌、放量、涨跌停、换手率异常、尾盘异动
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectService {

    private final StockDataService stockDataService;

    /** 上一轮行情快照，用于检测异动 */
    private final Map<String, StockQuote> previousQuotes = new ConcurrentHashMap<>();

    /** 异动事件缓存（防止重复通知） */
    private final Map<String, Long> anomalyCache = new ConcurrentHashMap<>();
    /** 同一类型异动的冷却时间（毫秒），默认30分钟 */
    private static final long COOLDOWN_MS = 30 * 60 * 1000;

    /**
     * 扫描股票池中的异动
     *
     * @return 异动事件列表
     */
    public List<AnomalyEvent> scanAnomalies() {
        List<String> stockPool = stockDataService.loadStockPool();
        if (stockPool.isEmpty()) return Collections.emptyList();

        List<StockQuote> quotes = stockDataService.getRealTimeQuotes(stockPool);
        List<AnomalyEvent> events = new ArrayList<>();

        for (StockQuote quote : quotes) {
            try {
                List<AnomalyEvent> stockEvents = detectStockAnomalies(quote);
                events.addAll(stockEvents);
            } catch (Exception e) {
                log.trace("监测异动异常: {}", quote.getCode(), e);
            }
        }

        // 更新快照
        for (StockQuote q : quotes) {
            previousQuotes.put(q.getCode(), q);
        }

        return events;
    }

    /**
     * 检测单只股票的异动
     */
    private List<AnomalyEvent> detectStockAnomalies(StockQuote quote) {
        List<AnomalyEvent> events = new ArrayList<>();
        String code = quote.getCode();
        String name = quote.getName();
        double changePercent = quote.getChangePercent();
        double price = quote.getCurrentPrice();

        // 1. 涨停检测（涨幅 >= 9.9%）
        if (changePercent >= 9.9) {
            events.add(createEvent(code, name, AnomalyEvent.AnomalyType.LIMIT_UP,
                    "涨停！当前价=" + price + " 涨幅=" + String.format("%.2f", changePercent) + "%",
                    price, changePercent, 0, 9.9));
        }
        // 跌停检测（跌幅 <= -9.9%）
        else if (changePercent <= -9.9) {
            events.add(createEvent(code, name, AnomalyEvent.AnomalyType.LIMIT_DOWN,
                    "跌停！当前价=" + price + " 跌幅=" + String.format("%.2f", changePercent) + "%",
                    price, changePercent, 0, -9.9));
        }

        // 2. 急速拉升/下跌检测（与上一轮快照对比）
        StockQuote prev = previousQuotes.get(code);
        if (prev != null && prev.getCurrentPrice() > 0) {
            double rapidChange = (quote.getCurrentPrice() - prev.getCurrentPrice()) / prev.getCurrentPrice() * 100;

            if (rapidChange >= 1.5) {
                events.add(createEvent(code, name, AnomalyEvent.AnomalyType.RAPID_RISE,
                        String.format("急速拉升 %.2f%% 当前价=%.2f", rapidChange, price),
                        price, changePercent, 0, 1.5));
            } else if (rapidChange <= -1.5) {
                events.add(createEvent(code, name, AnomalyEvent.AnomalyType.RAPID_FALL,
                        String.format("急速下跌 %.2f%% 当前价=%.2f", rapidChange, price),
                        price, changePercent, 0, -1.5));
            }
        }

        // 3. 换手率异常（> 10%）
        if (quote.getTurnoverRate() > 10) {
            events.add(createEvent(code, name, AnomalyEvent.AnomalyType.TURNOVER_ANOMALY,
                    String.format("换手率异常 %.2f%%", quote.getTurnoverRate()),
                    price, changePercent, 0, 10));
        }

        // 4. 尾盘异动（14:30后，涨跌幅绝对值 > 5%）
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        if (hour == 14 && minute >= 30 || hour == 15) {
            if (Math.abs(changePercent) >= 5 && Math.abs(changePercent) < 9.9) {
                String direction = changePercent > 0 ? "尾盘拉升" : "尾盘跳水";
                events.add(createEvent(code, name, AnomalyEvent.AnomalyType.LATE_SESSION_ANOMALY,
                        String.format("%s %.2f%%", direction, changePercent),
                        price, changePercent, 0, 5));
            }
        }

        return events;
    }

    /**
     * 创建异动事件（带冷却去重）
     */
    private AnomalyEvent createEvent(String code, String name, AnomalyEvent.AnomalyType type,
                                     String description, double price, double changePercent,
                                     double volumeRatio, double threshold) {
        String cacheKey = code + "_" + type.name();
        Long lastTime = anomalyCache.get(cacheKey);

        // 冷却期内不重复报警
        if (lastTime != null && (System.currentTimeMillis() - lastTime) < COOLDOWN_MS) {
            return null;
        }
        anomalyCache.put(cacheKey, System.currentTimeMillis());

        return AnomalyEvent.builder()
                .code(code)
                .name(name)
                .anomalyType(type)
                .description(description)
                .currentPrice(price)
                .changePercent(changePercent)
                .volumeRatio(volumeRatio)
                .threshold(threshold)
                .detectTime(LocalDateTime.now())
                .build();
    }
}
