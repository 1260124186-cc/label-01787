package com.xiaoan.bookstore.dto;

import lombok.Data;

import java.util.List;

@Data
public class SeasonVO {
    private Long id;
    private String title;
    private String subtitle;
    private String coverImage;
    private String description;
    private Integer seasonType;
    private Integer status;
    private String statusName;
    private String startDate;
    private String endDate;
    private String signupStart;
    private String signupEnd;
    private Integer durationDays;
    private Integer dailyMinDuration;
    private Integer dailyMaxDuration;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Integer pointsReward;
    private String badgeIcon;
    private String badgeName;
    private String rules;
    private String prizeConfig;
    private Boolean isJoined;
    private SeasonParticipantVO participantInfo;
    private List<SeasonBadgeVO> badges;
}
