package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class GroupRankVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private Long totalDuration;
    private String formattedDuration;
    private String bookTitle;
    private Integer rank;
}
