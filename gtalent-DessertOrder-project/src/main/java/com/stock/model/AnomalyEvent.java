package com.stock.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 异动事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyEvent {

    /** 股票代码 */
    private String code;

    /** 股票名称 */
    private String name;

    /** 异动类型 */
    private AnomalyType anomalyType;

    /** 异动描述 */
    private String description;

    /** 当前价格 */
    private double currentPrice;

    /** 涨跌幅 */
    private double changePercent;

    /** 成交量（相对于均量的倍数） */
    private double volumeRatio;

    /** 触发阈值 */
    private double threshold;

    /** 发现时间 */
    private LocalDateTime detectTime;

    public enum AnomalyType {
        /** 急速拉升 */
        RAPID_RISE,
        /** 急速下跌 */
        RAPID_FALL,
        /** 放量上涨 */
        VOLUME_UP,
        /** 缩量下跌 */
        VOLUME_DOWN,
        /** 封涨停 */
        LIMIT_UP,
        /** 跌停 */
        LIMIT_DOWN,
        /** 大单买入 */
        BIG_BUY,
        /** 大单卖出 */
        BIG_SELL,
        /** 换手率异常 */
        TURNOVER_ANOMALY,
        /** 尾盘异动 */
        LATE_SESSION_ANOMALY
    }
}
