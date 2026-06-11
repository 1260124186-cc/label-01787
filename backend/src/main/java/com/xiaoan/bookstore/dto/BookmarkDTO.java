package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookmarkDTO {
    @NotNull(message = "书籍ID不能为空")
    private Long bookId;
    private String bookTitle;
    @NotNull(message = "页码不能为空")
    private Integer pageNum;
    private Integer unitType;
    private String title;
    private String remark;
    private Integer sortOrder;
    private Integer isChapter;
}
