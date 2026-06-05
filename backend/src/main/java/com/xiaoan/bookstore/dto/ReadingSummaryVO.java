package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ReadingSummaryVO {
    /** 总阅读时长(秒) */
    private Long totalDuration;
    /** 阅读书籍数 */
    private Integer bookCount;
    /** 每日阅读数据 */
    private List<Map<String, Object>> dailyData;
    /** 统计周期 week/month/year */
    private String period;
    /** 周期开始 */
    private String periodStart;
    /** 周期结束 */
    private String periodEnd;
}
