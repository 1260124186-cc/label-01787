package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("excerpt_report")
public class ExcerptReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long excerptId;
    private Long reporterId;
    private String reason;
    private String detail;
    private Integer status;
    private Long handlerId;
    private String handleResult;
    private LocalDateTime handledAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
