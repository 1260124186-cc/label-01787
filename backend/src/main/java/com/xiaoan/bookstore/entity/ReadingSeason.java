package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reading_season")
public class ReadingSeason {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String subtitle;
    private String coverImage;
    private String description;
    private Integer seasonType;
    private Integer status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate signupStart;
    private LocalDate signupEnd;
    private Integer durationDays;
    private Integer dailyMinDuration;
    private Integer dailyMaxDuration;
    private Integer maxParticipants;
    private Integer pointsReward;
    private String badgeIcon;
    private String badgeName;
    private String rules;
    private String prizeConfig;
    private Integer cheatThresholdSpeed;
    private Integer cheatThresholdStreak;
    private Integer cheatAutoFlag;
    private LocalDateTime publishedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
