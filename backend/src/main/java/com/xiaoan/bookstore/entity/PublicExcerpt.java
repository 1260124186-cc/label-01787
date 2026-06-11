package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("public_excerpt")
public class PublicExcerpt {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long annotationId;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String excerptText;
    private String commentText;
    private Integer likes;
    private Integer favorites;
    private Integer views;
    private Integer status;
    private Integer auditStatus;
    private Integer reportCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
