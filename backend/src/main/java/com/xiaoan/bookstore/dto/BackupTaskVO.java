package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class BackupTaskVO {
    private Long id;
    private Long userId;
    private String userNickname;
    private Integer taskType;
    private String taskTypeText;
    private Integer status;
    private String statusText;
    private String fileName;
    private Long fileSize;
    private String fileSizeText;
    private Integer bookCount;
    private Integer annotationCount;
    private Integer recordCount;
    private Integer categoryCount;
    private Integer progress;
    private String errorMessage;
    private String createdAt;
    private String updatedAt;
    private String expiredAt;
}
