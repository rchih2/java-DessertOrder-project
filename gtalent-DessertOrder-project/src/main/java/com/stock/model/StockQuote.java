package com.stock.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 实时行情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQuote {

    /** 股票代码 */
    private String code;

    /** 股票名称 */
    private String name;

    /** 当前价 */
    private double currentPrice;

    /** 今开 */
    private double openPrice;

    /** 昨收 */
    private double preClose;

    /** 最高 */
    private double high;

    /** 最低 */
    private double low;

    /** 成交量（手） */
    private long volume;

    /** 成交额（万） */
    private double amount;

    /** 涨跌幅(%) */
    private double changePercent;

    /** 换手率(%) */
    private double turnoverRate;

    /** 市盈率 */
    private double pe;

    /** 市净率 */
    private double pb;
}
