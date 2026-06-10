package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class GroupMemberVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer role;
    private Integer readingPublic;
    private Long totalDuration;
    private String bookTitle;
    private java.time.LocalDateTime joinedAt;
}
