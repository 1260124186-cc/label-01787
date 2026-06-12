package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClassroomCreateDTO {
    @NotBlank(message = "班级名称不能为空")
    @Size(max = 100, message = "班级名称不能超过100个字符")
    private String name;
    @Size(max = 500, message = "班级描述不能超过500个字符")
    private String description;
    @Size(max = 200, message = "所属机构不能超过200个字符")
    private String institution;
    @Size(max = 50, message = "年级不能超过50个字符")
    private String gradeLevel;
}
