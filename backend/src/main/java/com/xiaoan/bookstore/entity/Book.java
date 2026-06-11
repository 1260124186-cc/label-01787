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
    private String coverThumbnail;
    private Long fileSize;
    private String bookFormat;
    private Integer pageCount;
    private Integer preRenderStatus;
    private Integer preRenderedPages;
    private String preRenderError;
    private Integer chapterCount;
    private Long categoryId;
    private Integer lastPage;
    private Integer lastChapter;
    private Integer copyrightDeclared;
    private LocalDateTime copyrightAgreedAt;
    private Integer status;
    private LocalDateTime lastReadAt;
    private LocalDateTime deletedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
