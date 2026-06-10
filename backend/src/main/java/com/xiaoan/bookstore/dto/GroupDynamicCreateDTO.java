package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GroupDynamicCreateDTO {
    @NotNull(message = "动态类型不能为空")
    private Integer type;
    @Size(max = 2000, message = "内容不能超过2000个字符")
    private String content;
    private String bookTitle;
    private String excerptText;
    private Integer duration;
}
