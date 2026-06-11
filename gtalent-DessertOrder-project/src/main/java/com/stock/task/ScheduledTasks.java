package com.stock.task;

import com.stock.model.AnomalyEvent;
import com.stock.model.NewsItem;
import com.stock.model.StockSignal;
import com.stock.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 定时任务 - 选股、异动监测、新闻推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final StockSelectService stockSelectService;
    private final AnomalyDetectService anomalyDetectService;
    private final NewsService newsService;
    private final DingTalkService dingTalkService;
    private final TencentAIService tencentAIService;

    /**
     * 盘中异动监测 - 每5分钟执行（交易日 9:30-15:00）
     */
    @Scheduled(cron = "${schedule.anomaly-monitor}")
    public void monitorAnomalies() {
        if (!isTradeTime()) {
            return;
        }
        log.info("=== 开始异动监测 ===");
        try {
            List<AnomalyEvent> events = anomalyDetectService.scanAnomalies();
            if (!events.isEmpty()) {
                log.info("发现 {} 个异动事件", events.size());
                dingTalkService.sendAnomalyAlert(events);
            }
        } catch (Exception e) {
            log.error("异动监测任务异常", e);
        }
    }

    /**
     * 收盘后选股 - 每天15:30
     */
    @Scheduled(cron = "${schedule.stock-select}")
    public void dailyStockSelection() {
        log.info("=== 开始执行选股策略 ===");
        try {
            List<String> periods = List.of("daily", "weekly", "monthly");
            List<StockSignal> signals = stockSelectService.executeStockSelection(periods);

            if (!signals.isEmpty()) {
                // 推送选股结果
                dingTalkService.sendStockSignals(signals);

                // AI点评
                String aiAnalysis = tencentAIService.analyzeStockSignals(signals);
                if (aiAnalysis != null && !aiAnalysis.isEmpty()) {
                    dingTalkService.sendMarkdown("AI选股点评", "### AI选股点评\n\n" + aiAnalysis);
                }
            } else {
                log.info("今日未发现选股信号");
                dingTalkService.sendText("今日收盘选股完成，未发现符合条件的信号。");
            }
        } catch (Exception e) {
            log.error("选股任务异常", e);
        }
    }

    /**
     * 早盘新闻推送 - 每天8:00
     */
    @Scheduled(cron = "${schedule.news-push}")
    public void morningNewsPush() {
        log.info("=== 开始获取财经要闻 ===");
        try {
            List<NewsItem> news = newsService.getAllImportantNews(15);

            if (!news.isEmpty()) {
                // 推送新闻摘要
                dingTalkService.sendNewsDigest(news);

                // AI分析
                String aiAnalysis = tencentAIService.analyzeNews(news);
                if (aiAnalysis != null && !aiAnalysis.isEmpty()) {
                    dingTalkService.sendMarkdown("AI市场分析", "### AI市场分析\n\n" + aiAnalysis);
                }
            }
        } catch (Exception e) {
            log.error("新闻推送任务异常", e);
        }
    }

    /**
     * 判断当前是否为交易时间
     */
    private boolean isTradeTime() {
        LocalTime now = LocalTime.now();
        int dayOfWeek = LocalDateTime.now().getDayOfWeek().getValue();
        // 周一到周五
        if (dayOfWeek > 5) return false;
        // 9:30 - 11:30, 13:00 - 15:00
        boolean morning = now.isAfter(LocalTime.of(9, 25)) && now.isBefore(LocalTime.of(11, 35));
        boolean afternoon = now.isAfter(LocalTime.of(12, 55)) && now.isBefore(LocalTime.of(15, 5));
        return morning || afternoon;
    }
}
