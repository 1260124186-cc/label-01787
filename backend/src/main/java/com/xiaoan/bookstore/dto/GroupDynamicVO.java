package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class GroupDynamicVO {
    private Long id;
    private Long groupId;
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer type;
    private String content;
    private String bookTitle;
    private String excerptText;
    private Integer duration;
    private String formattedDuration;
    private Integer likes;
    private Boolean liked;
    private java.time.LocalDateTime createdAt;
}
