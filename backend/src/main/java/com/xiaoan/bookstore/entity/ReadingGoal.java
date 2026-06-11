package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reading_goal")
public class ReadingGoal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer dailyGoalMinutes;
    private Integer weeklyGoalMinutes;
    private Integer goalType;
    private Integer remindEnabled;
    private String remindTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
