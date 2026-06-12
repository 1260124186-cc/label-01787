package com.xiaoan.bookstore.dto;

import lombok.Data;

import java.util.List;

@Data
public class SeasonProgressVO {
    private Long seasonId;
    private String title;
    private Integer status;
    private Integer durationDays;
    private Integer daysElapsed;
    private Integer daysRemaining;
    private Integer dailyMinDuration;
    private Integer qualifiedDays;
    private Long totalDuration;
    private Integer streakDays;
    private Integer maxStreakDays;
    private Double progress;
    private Integer rank;
    private List<String> qualifiedDates;
    private List<SeasonDailyRecordVO> dailyRecords;
}
