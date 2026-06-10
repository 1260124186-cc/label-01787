package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("points_exchange")
public class PointsExchange {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer exchangeType;
    private Integer pointsCost;
    private Integer value;
    private String orderNo;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
