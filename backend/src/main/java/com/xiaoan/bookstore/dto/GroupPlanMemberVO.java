package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class GroupPlanMemberVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private Long bookId;
    private Integer totalDuration;
    private String formattedDuration;
    private java.time.LocalDateTime lastReadAt;
    private java.time.LocalDateTime joinedAt;
}
