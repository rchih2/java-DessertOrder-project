package com.stock.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 选股信号
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSignal {

    /** 股票代码 */
    private String code;

    /** 股票名称 */
    private String name;

    /** 信号类型 */
    private SignalType signalType;

    /** 信号描述 */
    private String description;

    /** 信号强度 1-5 */
    private int strength;

    /** 当前价 */
    private double currentPrice;

    /** 涨跌幅 */
    private double changePercent;

    /** 周期: daily/weekly/monthly */
    private String period;

    /** MACD指标详情 */
    private MACDResult macdResult;

    /** 发现时间 */
    private LocalDateTime detectTime;

    /** 附加信息（如成交量异动等） */
    private String extraInfo;

    public enum SignalType {
        /** MACD金叉 */
        MACD_GOLDEN_CROSS,
        /** MACD零轴上金叉（强势） */
        MACD_ZERO_CROSS_GOLDEN,
        /** MACD底背离 */
        MACD_BOTTOM_DIVERGENCE,
        /** 底部放量 */
        VOLUME_BREAKOUT,
        /** 均线多头排列 */
        MA_BULLISH,
        /** 综合信号 */
        COMPOSITE
    }
}
