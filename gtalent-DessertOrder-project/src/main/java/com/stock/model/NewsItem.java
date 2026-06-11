package com.stock.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 财经新闻
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsItem {

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 来源 */
    private String source;

    /** URL */
    private String url;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 相关股票代码 */
    private String relatedCode;

    /** 分类: macro(宏观), industry(行业), stock(个股) */
    private String category;
}
