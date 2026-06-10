package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupPlanJoinDTO {
    @NotNull(message = "书籍ID不能为空")
    private Long bookId;
}
