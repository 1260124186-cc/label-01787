package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bilingual_alignment")
public class BilingualAlignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pairId;
    private Integer leftUnitIndex;
    private Integer rightUnitIndex;
    private Integer leftParagraphIndex;
    private Integer rightParagraphIndex;
    private String leftTextHash;
    private String rightTextHash;
    private Integer alignmentMethod;
    private BigDecimal confidence;
    private String leftTextSnippet;
    private String rightTextSnippet;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
