package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("copyright_complaint")
public class CopyrightComplaint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String complainantName;
    private String complainantContact;
    private Long bookId;
    private String bookTitle;
    private String reason;
    private String evidenceUrls;
    private Integer status;
    private Long handlerId;
    private String handleResult;
    private LocalDateTime handledAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
