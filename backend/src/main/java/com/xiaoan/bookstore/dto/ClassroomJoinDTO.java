package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassroomJoinDTO {
    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;
    private String studentNo;
    private String realName;
}
