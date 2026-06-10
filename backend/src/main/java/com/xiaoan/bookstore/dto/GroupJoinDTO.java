package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupJoinDTO {
    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;
}
