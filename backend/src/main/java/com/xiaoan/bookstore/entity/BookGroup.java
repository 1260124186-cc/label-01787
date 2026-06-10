package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("book_group")
public class BookGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String inviteCode;
    private Long creatorId;
    private Integer memberCount;
    private Integer status;
    private String banReason;
    private LocalDateTime bannedAt;
    private Long bannedBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
