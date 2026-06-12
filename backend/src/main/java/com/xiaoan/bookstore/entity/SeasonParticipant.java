package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("season_participant")
public class SeasonParticipant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long seasonId;
    private Long userId;
    private Integer status;
    private LocalDateTime signupAt;
    private Integer qualifiedDays;
    private Long totalDuration;
    private Integer totalBooks;
    private Integer streakDays;
    private Integer maxStreakDays;
    private LocalDateTime completedAt;
    private Integer pointsAwarded;
    private Integer badgeAwarded;
    private Integer prizeClaimed;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
