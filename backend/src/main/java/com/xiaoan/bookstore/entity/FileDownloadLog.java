package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("file_download_log")
public class FileDownloadLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer userType;
    private String fileToken;
    private String filePath;
    private String ip;
    private String referer;
    private String userAgent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
