package com.xiaoan.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class GroupPlanCreateDTO {
    @NotBlank(message = "共读书名不能为空")
    @Size(max = 200, message = "书名不能超过200个字符")
    private String bookTitle;
    @Size(max = 100, message = "作者名不能超过100个字符")
    private String bookAuthor;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    @Size(max = 500, message = "计划描述不能超过500个字符")
    private String description;
}
