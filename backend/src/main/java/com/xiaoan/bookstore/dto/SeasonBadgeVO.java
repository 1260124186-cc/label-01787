package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class SeasonBadgeVO {
    private Long id;
    private String badgeType;
    private String badgeName;
    private String badgeIcon;
    private String earnedAt;
}
