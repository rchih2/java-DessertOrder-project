package com.stock.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * K线数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KLineData {

    /** 日期，如 20250320 */
    private String date;

    /** 开盘价 */
    private double open;

    /** 收盘价 */
    private double close;

    /** 最高价 */
    private double high;

    /** 最低价 */
    private double low;

    /** 成交量（手） */
    private long volume;

    /** 成交额（元） */
    private double amount;

    /** 涨跌幅(%) */
    private double changePercent;

    /** 涨跌额 */
    private double changeAmount;
}
