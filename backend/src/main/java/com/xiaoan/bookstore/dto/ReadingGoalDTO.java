package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class ReadingGoalDTO {
    private Integer dailyGoalMinutes;
    private Integer weeklyGoalMinutes;
    private Integer goalType;
    private Integer remindEnabled;
    private String remindTime;
}
