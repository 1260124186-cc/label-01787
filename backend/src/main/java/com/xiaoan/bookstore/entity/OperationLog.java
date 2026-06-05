package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer userType;
    private String action;
    private String target;
    private String detail;
    private String ip;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
