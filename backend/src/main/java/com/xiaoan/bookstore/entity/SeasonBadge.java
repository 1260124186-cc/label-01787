package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("season_badge")
public class SeasonBadge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long seasonId;
    private String badgeType;
    private String badgeName;
    private String badgeIcon;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime earnedAt;
}
