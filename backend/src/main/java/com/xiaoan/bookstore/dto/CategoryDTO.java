package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryDTO {
    @NotBlank(message = "分类名称不能为空")
    private String name;
    private String color;
    private String icon;
    private Integer sortOrder;
}
