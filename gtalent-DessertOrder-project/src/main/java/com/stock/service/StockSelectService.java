package com.stock.service;

import com.stock.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 选股策略引擎
 * 支持: MACD金叉、零轴上金叉、底背离、放量突破、均线多头排列
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockSelectService {

    private final StockDataService stockDataService;
    private final IndicatorService indicatorService;

    /**
     * 执行全量选股（遍历股票池）
     *
     * @param periods K线周期列表: daily, weekly, monthly
     * @return 选股信号列表
     */
    public List<StockSignal> executeStockSelection(List<String> periods) {
        List<String> stockPool = stockDataService.loadStockPool();
        if (stockPool.isEmpty()) {
            log.warn("股票池为空，跳过选股");
            return Collections.emptyList();
        }

        log.info("开始选股，股票池数量={}, 周期={}", stockPool.size(), periods);
        List<StockSignal> allSignals = new ArrayList<>();

        for (String code : stockPool) {
            try {
                // 获取实时行情（用于补充价格信息）
                Optional<StockQuote> quoteOpt = stockDataService.getRealTimeQuote(code);
                String name = quoteOpt.map(StockQuote::getName).orElse(code);
                double price = quoteOpt.map(StockQuote::getCurrentPrice).orElse(0.0);
                double change = quoteOpt.map(StockQuote::getChangePercent).orElse(0.0);

                for (String period : periods) {
                    List<StockSignal> signals = analyzeStock(code, name, price, change, period);
                    allSignals.addAll(signals);
                }
                // 避免请求过快
                Thread.sleep(300);
            } catch (Exception e) {
                log.error("分析股票失败: {}", code, e);
            }
        }

        // 按信号强度排序
        allSignals.sort((a, b) -> b.getStrength() - a.getStrength());
        log.info("选股完成，共发现{}个信号", allSignals.size());
        return allSignals;
    }

    /**
     * 分析单只股票在指定周期下的信号
     */
    private List<StockSignal> analyzeStock(String code, String name, double price,
                                            double changePercent, String period) {
        List<StockSignal> signals = new ArrayList<>();

        List<KLineData> klineData = stockDataService.getKLineData(code, period);
        if (klineData.size() < 35) return signals;

        // 1. MACD分析
        List<MACDResult> macdResults = indicatorService.calculateMACD(klineData);
        if (!macdResults.isEmpty()) {
            MACDResult latest = macdResults.get(macdResults.size() - 1);

            // 1a. MACD金叉
            if (latest.isGoldenCross()) {
                String desc = String.format("%s周期MACD金叉 DIF=%.4f DEA=%.4f",
                        periodName(period), latest.getDif(), latest.getDea());
                int strength = 3;

                // 零轴上金叉 → 强度+1
                if (latest.getDif() > 0 && latest.getDea() > 0) {
                    desc = "【强势】" + desc + " (零轴上方)";
                    strength = 4;
                    signals.add(StockSignal.builder()
                            .code(code).name(name)
                            .signalType(StockSignal.SignalType.MACD_ZERO_CROSS_GOLDEN)
                            .description(desc).strength(strength)
                            .currentPrice(price).changePercent(changePercent)
                            .period(period).macdResult(latest)
                            .detectTime(LocalDateTime.now())
                            .build());
                }

                signals.add(StockSignal.builder()
                        .code(code).name(name)
                        .signalType(StockSignal.SignalType.MACD_GOLDEN_CROSS)
                        .description(desc).strength(strength)
                        .currentPrice(price).changePercent(changePercent)
                        .period(period).macdResult(latest)
                        .detectTime(LocalDateTime.now())
                        .build());
            }

            // 1b. MACD底背离
            if (indicatorService.detectMACDBottomDivergence(klineData, macdResults)) {
                signals.add(StockSignal.builder()
                        .code(code).name(name)
                        .signalType(StockSignal.SignalType.MACD_BOTTOM_DIVERGENCE)
                        .description(periodName(period) + "周期MACD底背离")
                        .strength(4)
                        .currentPrice(price).changePercent(changePercent)
                        .period(period).macdResult(latest)
                        .detectTime(LocalDateTime.now())
                        .build());
            }
        }

        // 2. 放量分析（量比 > 2）
        if (indicatorService.isVolumeBreakout(klineData, 20, 2.0)) {
            double volumeRatio = indicatorService.calculateVolumeRatio(klineData, 20);
            signals.add(StockSignal.builder()
                    .code(code).name(name)
                    .signalType(StockSignal.SignalType.VOLUME_BREAKOUT)
                    .description(periodName(period) + "周期放量突破 量比=" + volumeRatio)
                    .strength(3)
                    .currentPrice(price).changePercent(changePercent)
                    .period(period)
                    .detectTime(LocalDateTime.now())
                    .extraInfo("量比: " + volumeRatio)
                    .build());
        }

        // 3. 均线多头排列（日线级别）
        if ("daily".equals(period) && indicatorService.isBullishMALayout(klineData)) {
            signals.add(StockSignal.builder()
                    .code(code).name(name)
                    .signalType(StockSignal.SignalType.MA_BULLISH)
                    .description("均线多头排列 MA5>MA10>MA20>MA60")
                    .strength(3)
                    .currentPrice(price).changePercent(changePercent)
                    .period(period)
                    .detectTime(LocalDateTime.now())
                    .build());
        }

        // 4. 综合评分（日线）
        if ("daily".equals(period)) {
            int compositeScore = 0;
            StringBuilder reasons = new StringBuilder();

            List<Double> rsi6 = indicatorService.calculateRSI(klineData, 6);
            if (!rsi6.isEmpty()) {
                double rsi = rsi6.get(rsi6.size() - 1);
                if (rsi < 30) {
                    compositeScore++;
                    reasons.append("RSI6超卖(").append(String.format("%.1f", rsi)).append(") ");
                }
            }

            List<double[]> kdj = indicatorService.calculateKDJ(klineData, 9);
            if (!kdj.isEmpty()) {
                double[] kd = kdj.get(kdj.size() - 1);
                if (kd[0] < 20 && kd[1] < 20) {
                    compositeScore++;
                    reasons.append("KDJ低位 ");
                }
            }

            List<Double> ma20 = indicatorService.calculateMA(klineData, 20);
            if (!ma20.isEmpty()) {
                double ma = ma20.get(ma20.size() - 1);
                if (price > 0 && price > ma) {
                    compositeScore++;
                    reasons.append("站上MA20 ");
                }
            }

            if (compositeScore >= 2) {
                signals.add(StockSignal.builder()
                        .code(code).name(name)
                        .signalType(StockSignal.SignalType.COMPOSITE)
                        .description("综合信号(" + compositeScore + "/3): " + reasons)
                        .strength(compositeScore)
                        .currentPrice(price).changePercent(changePercent)
                        .period(period)
                        .detectTime(LocalDateTime.now())
                        .build());
            }
        }

        return signals;
    }

    private String periodName(String period) {
        return switch (period) {
            case "weekly" -> "周线";
            case "monthly" -> "月线";
            default -> "日线";
        };
    }
}
