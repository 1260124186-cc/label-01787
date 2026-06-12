package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignmentCreateDTO {
    @NotBlank(message = "书名不能为空")
    private String bookTitle;
    private String bookAuthor;
    private Long bookId;
    @NotNull(message = "起始页码不能为空")
    private Integer startPage;
    @NotNull(message = "结束页码不能为空")
    private Integer endPage;
    @NotNull(message = "截止日期不能为空")
    private String deadline;
    private String description;
    private Integer totalScore;
}
