package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GradeDTO {
    @NotNull(message = "评分不能为空")
    private Integer score;
    private String teacherComment;
}
