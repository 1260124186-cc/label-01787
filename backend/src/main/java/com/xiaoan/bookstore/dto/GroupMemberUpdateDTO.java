package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupMemberUpdateDTO {
    @NotNull(message = "阅读公开状态不能为空")
    private Integer readingPublic;
}
