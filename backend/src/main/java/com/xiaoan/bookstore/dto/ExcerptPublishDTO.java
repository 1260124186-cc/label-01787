package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExcerptPublishDTO {
    @NotNull(message = "批注ID不能为空")
    private Long annotationId;
    @Size(max = 500, message = "评语不能超过500个字符")
    private String commentText;
}
