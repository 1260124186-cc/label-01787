package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ReadingReportVO {
    private Long id;
    private String reportType;
    private String periodStart;
    private String periodEnd;
    private Long totalDuration;
    private String totalDurationText;
    private Integer bookCount;
    private Integer finishedBookCount;
    private Integer annotationCount;
    private Integer maxStreakDays;
    private Integer readingDays;
    private Integer currentStreakDays;
    private Integer shareCount;
    private List<Map<String, Object>> bookRank;
    private List<Map<String, Object>> categoryStats;
    private List<Map<String, Object>> dailyData;
    private String avgDailyDuration;
    private String maxDayDuration;
    private String maxDayDate;
    private Map<String, Object> compareData;
}
