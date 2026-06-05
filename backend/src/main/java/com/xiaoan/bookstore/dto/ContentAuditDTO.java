package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class ContentAuditDTO {
    private Integer targetType;
    private Long targetId;
    private String content;
}
