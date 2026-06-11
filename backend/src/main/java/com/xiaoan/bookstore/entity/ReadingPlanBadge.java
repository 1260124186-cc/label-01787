package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reading_plan_badge")
public class ReadingPlanBadge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long planId;
    private String badgeType;
    private String badgeName;
    private String badgeIcon;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime earnedAt;
}
