package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("family")
public class Family {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String inviteCode;
    private Long ownerId;
    private Integer memberCount;
    private Integer maxMembers;
    private Long sharedStorage;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
