package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reading_report")
public class ReadingReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String reportType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Long totalDuration;
    private Integer bookCount;
    private Integer annotationCount;
    private Integer maxStreakDays;
    private Integer readingDays;
    private String reportData;
    private Integer shareCount;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
