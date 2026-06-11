package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnnotationDTO {
    @NotNull(message = "书籍ID不能为空")
    private Long bookId;
    @NotNull(message = "页码不能为空")
    private Integer pageNum;
    private String selectedText;
    @NotBlank(message = "内容不能为空")
    private String content;
    @NotNull(message = "类型不能为空")
    private Integer type;
    private String tags;
    private Integer isPinned;
    private String color;
}
