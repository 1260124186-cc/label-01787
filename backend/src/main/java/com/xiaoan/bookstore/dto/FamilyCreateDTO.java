package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FamilyCreateDTO {
    @NotBlank(message = "家庭名称不能为空")
    @Size(max = 100, message = "家庭名称不能超过100个字符")
    private String name;
}
