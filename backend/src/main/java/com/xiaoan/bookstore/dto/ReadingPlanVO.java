package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReadingPlanVO {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookFormat;
    private Integer targetDays;
    private Integer dailyMinDuration;
    private String reminderTime;
    private Integer readPages;
    private Integer totalPages;
    private Integer streakDays;
    private Integer maxStreakDays;
    private Integer status;
    private String startDate;
    private String endDate;
    private String estimatedEndDate;
    private Double progress;
    private Integer avgDailyPages;
    private List<String> badges;
}
