package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("book")
public class Book {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String author;
    private String filePath;
    private Long fileSize;
    private Integer pageCount;
    private Long categoryId;
    private Integer lastPage;
    private Integer copyrightDeclared;
    private LocalDateTime copyrightAgreedAt;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
