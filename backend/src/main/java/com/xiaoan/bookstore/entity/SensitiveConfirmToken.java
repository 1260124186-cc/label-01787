package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sensitive_confirm_token")
public class SensitiveConfirmToken {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private String token;
    private String operation;
    private LocalDateTime expiredAt;
    private Integer used;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
