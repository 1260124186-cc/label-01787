package com.xiaoan.bookstore.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SeasonUpdateDTO {
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
}
