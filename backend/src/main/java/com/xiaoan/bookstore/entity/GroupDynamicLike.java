package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_dynamic_like")
public class GroupDynamicLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dynamicId;
    private Long userId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
