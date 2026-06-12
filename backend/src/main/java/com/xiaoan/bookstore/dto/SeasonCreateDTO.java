package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SeasonCreateDTO {
    @NotBlank(message = "赛季标题不能为空")
    private String title;
    private String subtitle;
    private String coverImage;
    private String description;
    private Integer seasonType;
    @NotNull(message = "赛季开始日期不能为空")
    private LocalDate startDate;
    @NotNull(message = "赛季结束日期不能为空")
    private LocalDate endDate;
    private LocalDate signupStart;
    private LocalDate signupEnd;
    @NotNull(message = "赛季天数不能为空")
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
}
