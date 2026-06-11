package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExcerptAuditDTO {
    @NotNull(message = "审核结果不能为空")
    private Integer auditStatus;
    private String reason;
}
