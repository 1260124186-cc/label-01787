package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reading_plan_checkin")
public class ReadingPlanCheckin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long userId;
    private LocalDate checkinDate;
    private Integer duration;
    private Integer pagesRead;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
