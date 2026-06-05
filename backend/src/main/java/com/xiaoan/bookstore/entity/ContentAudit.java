package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("content_audit")
public class ContentAudit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer targetType;
    private Long targetId;
    private String content;
    private Integer result;
    private String keywords;
    private Long auditorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
