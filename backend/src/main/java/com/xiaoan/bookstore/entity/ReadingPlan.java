package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reading_plan")
public class ReadingPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer targetDays;
    private Integer dailyMinDuration;
    private String reminderTime;
    private Integer readPages;
    private Integer totalPages;
    private Integer streakDays;
    private Integer maxStreakDays;
    private Integer status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime completedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
