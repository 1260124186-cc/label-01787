package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ReadingGoalProgressVO {
    private Integer dailyGoalMinutes;
    private Integer weeklyGoalMinutes;
    private Integer goalType;
    private Long todayDuration;
    private Long weekDuration;
    private Double dailyProgress;
    private Double weeklyProgress;
    private Integer currentStreakDays;
    private Integer maxStreakDays;
    private Boolean dailyCompleted;
    private Boolean weeklyCompleted;
    private List<Map<String, Object>> bookRank;
    private List<Map<String, Object>> categoryStats;
    private Integer annotationCount;
    private Integer finishedBookCount;
}
