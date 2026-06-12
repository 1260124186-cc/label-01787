package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("season_cheat_detection")
public class SeasonCheatDetection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long seasonId;
    private Long userId;
    private LocalDate detectionDate;
    private Integer detectionType;
    private String detectionDetail;
    private Integer severity;
    private Integer status;
    private Long handledBy;
    private String handleResult;
    private LocalDateTime handledAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
