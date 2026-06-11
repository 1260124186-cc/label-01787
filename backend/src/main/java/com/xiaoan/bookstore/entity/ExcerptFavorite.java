package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("excerpt_favorite")
public class ExcerptFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long excerptId;
    private Long userId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
