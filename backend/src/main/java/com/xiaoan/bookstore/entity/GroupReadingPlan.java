package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("group_reading_plan")
public class GroupReadingPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private String bookTitle;
    private String bookAuthor;
    private Long creatorId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
