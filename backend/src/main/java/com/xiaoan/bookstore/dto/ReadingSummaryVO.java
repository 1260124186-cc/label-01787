package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ReadingSummaryVO {
    private Long totalDuration;
    private Integer bookCount;
    private List<Map<String, Object>> dailyData;
    private String period;
    private String periodStart;
    private String periodEnd;
    private Boolean isVip;
    private List<Map<String, Object>> bookRank;
    private List<Map<String, Object>> categoryStats;
    private String avgDailyDuration;
    private String maxDayDuration;
    private String maxDayDate;
    private Integer readingDays;
    private Double avgPagesPerDay;
}
