package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("points_record")
public class PointsRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer type;
    private String category;
    private Integer points;
    private Integer balanceAfter;
    private String description;
    private String refId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
