package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_membership")
public class UserMembership {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String planCode;
    private LocalDateTime expireAt;
    private Integer autoRenew;
    private Long extraStorage;
    private Integer aiUsedToday;
    private String aiUsageDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
