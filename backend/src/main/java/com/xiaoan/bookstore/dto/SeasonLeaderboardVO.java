package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class SeasonLeaderboardVO {
    private Integer rank;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer qualifiedDays;
    private Long totalDuration;
    private Integer streakDays;
    private String formattedDuration;
}
