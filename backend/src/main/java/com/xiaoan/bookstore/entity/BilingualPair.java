package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bilingual_pair")
public class BilingualPair {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long leftBookId;
    private Long rightBookId;
    private String leftLanguage;
    private String rightLanguage;
    private Integer alignmentStrategy;
    private String name;
    private Integer lastLeftUnit;
    private Integer lastRightUnit;
    private Integer leftUnitType;
    private Integer rightUnitType;
    private Integer syncEnabled;
    private Integer aiAlignmentStatus;
    private Integer aiAlignmentProgress;
    private String aiAlignmentError;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
