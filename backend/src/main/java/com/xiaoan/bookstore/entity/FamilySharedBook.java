package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("family_shared_book")
public class FamilySharedBook {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long familyId;
    private Long bookId;
    private Long sharedBy;
    private LocalDateTime sharedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
