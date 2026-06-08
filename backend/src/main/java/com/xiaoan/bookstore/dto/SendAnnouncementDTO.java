package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class SendAnnouncementDTO {
    
    @NotNull(message = "消息类型不能为空")
    private Integer type;
    
    @NotBlank(message = "标题不能为空")
    private String title;
    
    @NotBlank(message = "内容不能为空")
    private String content;
    
    private List<Long> userIds;
    
    private Boolean sendToAll;
    
    private String extraData;
}
