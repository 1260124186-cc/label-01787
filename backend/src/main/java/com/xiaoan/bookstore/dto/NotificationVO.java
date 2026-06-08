package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private Long id;
    private Integer type;
    private String typeName;
    private String title;
    private String content;
    private String extraData;
    private Integer isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
