package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class SeasonParticipantVO {
    private Long id;
    private Long seasonId;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer status;
    private String statusName;
    private String signupAt;
    private Integer qualifiedDays;
    private Long totalDuration;
    private Integer totalBooks;
    private Integer streakDays;
    private Integer maxStreakDays;
    private String completedAt;
    private Integer pointsAwarded;
    private Integer badgeAwarded;
    private Integer prizeClaimed;
    private Integer rank;
    private Double progress;
}
