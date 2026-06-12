package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("season_daily_record")
public class SeasonDailyRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long seasonId;
    private Long userId;
    private LocalDate recordDate;
    private Integer duration;
    private Integer bookCount;
    private Integer isQualified;
    private Integer isFlagged;
    private String flagReason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
