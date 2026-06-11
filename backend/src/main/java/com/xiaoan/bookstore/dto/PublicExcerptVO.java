package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PublicExcerptVO {
    private Long id;
    private Long userId;
    private String nickname;
    private String avatar;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String excerptText;
    private String commentText;
    private Integer likes;
    private Integer favorites;
    private Integer views;
    private Boolean liked;
    private Boolean favorited;
    private Integer status;
    private Integer auditStatus;
    private LocalDateTime createdAt;
}
