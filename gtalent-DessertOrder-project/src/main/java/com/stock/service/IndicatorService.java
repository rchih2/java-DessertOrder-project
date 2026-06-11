package com.stock.service;

import com.stock.model.KLineData;
import com.stock.model.MACDResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 技术指标计算引擎
 * 包含: MACD, MA, RSI, KDJ, 成交量分析
 */
@Slf4j
@Service
public class IndicatorService {

    // ========== MACD ==========

    /**
     * 计算MACD指标
     *
     * @param dataList K线数据（需按时间升序，建议至少60根）
     * @param fastPeriod   快线周期，默认12
     * @param slowPeriod   慢线周期，默认26
     * @param signalPeriod 信号线周期，默认9
     * @return MACD结果列表
     */
    public List<MACDResult> calculateMACD(List<KLineData> dataList,
                                          int fastPeriod, int slowPeriod, int signalPeriod) {
        if (dataList == null || dataList.size() < slowPeriod + signalPeriod) {
            log.warn("K线数据不足，无法计算MACD: size={}, required>={}",
                    dataList == null ? 0 : dataList.size(), slowPeriod + signalPeriod);
            return Collections.emptyList();
        }

        int size = dataList.size();
        double[] closes = dataList.stream().mapToDouble(KLineData::getClose).toArray();

        // 1. 计算EMA
        double[] emaFast = calculateEMA(closes, fastPeriod);
        double[] emaSlow = calculateEMA(closes, slowPeriod);

        // 2. 计算DIF
        double[] dif = new double[size];
        for (int i = 0; i < size; i++) {
            dif[i] = emaFast[i] - emaSlow[i];
        }

        // 3. 计算DEA（DIF的EMA）
        double[] dea = calculateEMA(dif, signalPeriod);

        // 4. 计算MACD柱状图并判断金叉/死叉
        List<MACDResult> results = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            double macdValue = 2.0 * (dif[i] - dea[i]);

            boolean goldenCross = false;
            boolean deathCross = false;
            int difDirection = 0;
            int deaDirection = 0;

            if (i > 0) {
                // 金叉: 前一日 DIF < DEA, 当日 DIF >= DEA
                goldenCross = (dif[i - 1] < dea[i - 1] && dif[i] >= dea[i]);
                // 死叉: 前一日 DIF > DEA, 当日 DIF <= DEA
                deathCross = (dif[i - 1] > dea[i - 1] && dif[i] <= dea[i]);

                difDirection = dif[i] > dif[i - 1] ? 1 : (dif[i] < dif[i - 1] ? -1 : 0);
                deaDirection = dea[i] > dea[i - 1] ? 1 : (dea[i] < dea[i - 1] ? -1 : 0);
            }

            results.add(MACDResult.builder()
                    .date(dataList.get(i).getDate())
                    .dif(round(dif[i], 4))
                    .dea(round(dea[i], 4))
                    .macd(round(macdValue, 4))
                    .goldenCross(goldenCross)
                    .deathCross(deathCross)
                    .difDirection(difDirection)
                    .deaDirection(deaDirection)
                    .build());
        }

        return results;
    }

    /**
     * 计算MACD（默认参数 12/26/9）
     */
    public List<MACDResult> calculateMACD(List<KLineData> dataList) {
        return calculateMACD(dataList, 12, 26, 9);
    }

    // ========== 均线 MA ==========

    /**
     * 计算移动平均线
     *
     * @param period 周期（如5, 10, 20, 60, 120, 250）
     */
    public List<Double> calculateMA(List<KLineData> dataList, int period) {
        if (dataList == null || dataList.size() < period) return Collections.emptyList();

        List<Double> result = new ArrayList<>();
        double[] closes = dataList.stream().mapToDouble(KLineData::getClose).toArray();

        for (int i = 0; i < closes.length; i++) {
            if (i < period - 1) {
                result.add(0.0);
            } else {
                double sum = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    sum += closes[j];
                }
                result.add(round(sum / period, 2));
            }
        }
        return result;
    }

    // ========== RSI ==========

    /**
     * 计算RSI指标
     *
     * @param period 周期，通常为6, 12, 24
     */
    public List<Double> calculateRSI(List<KLineData> dataList, int period) {
        if (dataList == null || dataList.size() < period + 1) return Collections.emptyList();

        List<Double> result = new ArrayList<>();
        double[] closes = dataList.stream().mapToDouble(KLineData::getClose).toArray();
        double avgGain = 0, avgLoss = 0;

        // 第一个周期的初始化
        for (int i = 1; i <= period; i++) {
            double change = closes[i] - closes[i - 1];
            if (change > 0) avgGain += change;
            else avgLoss += Math.abs(change);
        }
        avgGain /= period;
        avgLoss /= period;

        result.add(0.0); // 第一个数据点
        for (int i = 0; i < period - 1; i++) result.add(0.0);
        result.add(round(100 - 100.0 / (1 + avgGain / (avgLoss == 0 ? 0.0001 : avgLoss)), 2));

        // 后续使用平滑法
        for (int i = period + 1; i < closes.length; i++) {
            double change = closes[i] - closes[i - 1];
            double gain = change > 0 ? change : 0;
            double loss = change < 0 ? Math.abs(change) : 0;
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
            result.add(round(100 - 100.0 / (1 + avgGain / (avgLoss == 0 ? 0.0001 : avgLoss)), 2));
        }

        return result;
    }

    // ========== KDJ ==========

    /**
     * 计算KDJ指标
     */
    public List<double[]> calculateKDJ(List<KLineData> dataList, int period) {
        List<double[]> result = new ArrayList<>();
        if (dataList == null || dataList.size() < period) return result;

        double prevK = 50, prevD = 50;

        for (int i = 0; i < dataList.size(); i++) {
            if (i < period - 1) {
                result.add(new double[]{50, 50, 50});
                continue;
            }

            double highest = Double.MIN_VALUE, lowest = Double.MAX_VALUE;
            for (int j = i - period + 1; j <= i; j++) {
                highest = Math.max(highest, dataList.get(j).getHigh());
                lowest = Math.min(lowest, dataList.get(j).getLow());
            }

            double rsv = (highest == lowest) ? 50 :
                    (dataList.get(i).getClose() - lowest) / (highest - lowest) * 100;

            double k = 2.0 / 3 * prevK + 1.0 / 3 * rsv;
            double d = 2.0 / 3 * prevD + 1.0 / 3 * k;
            double j = 3 * k - 2 * d;

            result.add(new double[]{round(k, 2), round(d, 2), round(j, 2)});
            prevK = k;
            prevD = d;
        }

        return result;
    }

    // ========== 成交量分析 ==========

    /**
     * 计算量比（当前成交量 / 过去N周期平均成交量）
     */
    public double calculateVolumeRatio(List<KLineData> dataList, int avgPeriod) {
        if (dataList == null || dataList.size() < avgPeriod + 1) return 0;

        double recentVolume = dataList.get(dataList.size() - 1).getVolume();
        double avgVolume = 0;
        for (int i = dataList.size() - avgPeriod - 1; i < dataList.size() - 1; i++) {
            avgVolume += dataList.get(i).getVolume();
        }
        avgVolume /= avgPeriod;

        return avgVolume == 0 ? 0 : round(recentVolume / avgVolume, 2);
    }

    /**
     * 判断是否放量（量比超过阈值）
     */
    public boolean isVolumeBreakout(List<KLineData> dataList, int avgPeriod, double threshold) {
        return calculateVolumeRatio(dataList, avgPeriod) >= threshold;
    }

    // ========== 均线多头/空头排列 ==========

    /**
     * 判断均线是否多头排列（MA5 > MA10 > MA20 > MA60）
     */
    public boolean isBullishMALayout(List<KLineData> dataList) {
        List<Double> ma5 = calculateMA(dataList, 5);
        List<Double> ma10 = calculateMA(dataList, 10);
        List<Double> ma20 = calculateMA(dataList, 20);
        List<Double> ma60 = calculateMA(dataList, 60);

        int last = dataList.size() - 1;
        return ma5.get(last) != null && ma10.get(last) != null
                && ma20.get(last) != null && ma60.get(last) != null
                && ma5.get(last) > ma10.get(last)
                && ma10.get(last) > ma20.get(last)
                && ma20.get(last) > ma60.get(last);
    }

    // ========== 底背离检测 ==========

    /**
     * 检测MACD底背离（价格创新低，MACD不创新低）
     */
    public boolean detectMACDBottomDivergence(List<KLineData> dataList, List<MACDResult> macdList) {
        if (dataList.size() < 20 || macdList.size() < 20) return false;

        // 找最近20根K线中的两个低点
        double priceLow1 = Double.MAX_VALUE, priceLow2 = Double.MAX_VALUE;
        double macdLow1 = Double.MAX_VALUE, macdLow2 = Double.MAX_VALUE;
        int idxLow1 = -1, idxLow2 = -1;

        int len = dataList.size();
        // 后半段低点
        for (int i = (int) (len * 0.5); i < len; i++) {
            if (dataList.get(i).getLow() < priceLow1) {
                priceLow1 = dataList.get(i).getLow();
                macdLow1 = macdList.get(i).getMacd();
                idxLow1 = i;
            }
        }
        // 前半段低点
        for (int i = (int) (len * 0.2); i < (int) (len * 0.5); i++) {
            if (dataList.get(i).getLow() < priceLow2) {
                priceLow2 = dataList.get(i).getLow();
                macdLow2 = macdList.get(i).getMacd();
                idxLow2 = i;
            }
        }

        // 底背离: 价格新低 < 前低, 但 MACD低 > 前低（底部抬高）
        return idxLow1 > idxLow2
                && priceLow1 < priceLow2
                && macdLow1 > macdLow2;
    }

    // ========== 私有工具方法 ==========

    /**
     * 计算指数移动平均线(EMA)
     */
    private double[] calculateEMA(double[] data, int period) {
        double[] ema = new double[data.length];
        double multiplier = 2.0 / (period + 1);

        // 第一个EMA值用SMA初始化
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += data[i];
        }
        ema[period - 1] = sum / period;

        for (int i = 0; i < period - 1; i++) {
            ema[i] = data[i]; // 数据不足时用原值
        }

        // 后续EMA
        for (int i = period; i < data.length; i++) {
            ema[i] = (data[i] - ema[i - 1]) * multiplier + ema[i - 1];
        }

        return ema;
    }

    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
