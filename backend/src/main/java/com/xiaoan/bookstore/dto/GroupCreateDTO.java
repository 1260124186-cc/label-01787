package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GroupCreateDTO {
    @NotBlank(message = "小组名称不能为空")
    @Size(max = 100, message = "小组名称不能超过100个字符")
    private String name;
    @Size(max = 500, message = "小组简介不能超过500个字符")
    private String description;
}
