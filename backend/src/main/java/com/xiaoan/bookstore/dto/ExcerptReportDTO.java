package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExcerptReportDTO {
    @NotNull(message = "书摘ID不能为空")
    private Long excerptId;
    @NotBlank(message = "举报原因不能为空")
    @Size(max = 200, message = "举报原因不能超过200个字符")
    private String reason;
    @Size(max = 1000, message = "详细描述不能超过1000个字符")
    private String detail;
}
