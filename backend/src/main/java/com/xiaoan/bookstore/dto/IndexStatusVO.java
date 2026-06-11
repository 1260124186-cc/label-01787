package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class IndexStatusVO {
    private Long id;
    private Long userId;
    private String userNickname;
    private Long bookId;
    private String bookTitle;
    private Integer totalPages;
    private Integer indexedPages;
    private Integer status;
    private String statusText;
    private String errorMessage;
    private Integer progress;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
