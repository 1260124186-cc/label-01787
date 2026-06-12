package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmissionDTO {
    @NotNull(message = "阅读时长不能为空")
    private Integer readingDuration;
    private String annotationSummary;
    private Integer pageProgress;
    private String proofImages;
}
