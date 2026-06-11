package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReadingPlanProgressVO {
    private Long planId;
    private Integer readPages;
    private Integer totalPages;
    private Double progress;
    private Integer streakDays;
    private Integer maxStreakDays;
    private String estimatedEndDate;
    private Integer avgDailyPages;
    private Integer remainingPages;
    private Integer remainingDays;
    private List<String> checkinDates;
}
