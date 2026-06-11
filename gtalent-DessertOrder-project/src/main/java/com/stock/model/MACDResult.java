package com.stock.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * MACD指标计算结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MACDResult {

    /** 日期 */
    private String date;

    /** DIF线（快线） */
    private double dif;

    /** DEA线（慢线/信号线） */
    private double dea;

    /** MACD柱状图 = 2 * (DIF - DEA) */
    private double macd;

    /** 金叉信号（DIF上穿DEA） */
    private boolean goldenCross;

    /** 死叉信号（DIF下穿DEA） */
    private boolean deathCross;

    /** DIF方向：1=向上, -1=向下, 0=持平 */
    private int difDirection;

    /** DEA方向 */
    private int deaDirection;
}
