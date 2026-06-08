package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationTemplateDTO {
    
    @NotBlank(message = "模板编码不能为空")
    private String code;
    
    @NotBlank(message = "模板名称不能为空")
    private String name;
    
    @NotNull(message = "消息类型不能为空")
    private Integer type;
    
    @NotBlank(message = "标题不能为空")
    private String title;
    
    @NotBlank(message = "内容不能为空")
    private String content;
    
    private Integer status;
}
