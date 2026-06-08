package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("backup_task")
public class BackupTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer taskType;
    private Integer status;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private Integer bookCount;
    private Integer annotationCount;
    private Integer recordCount;
    private Integer categoryCount;
    private Integer progress;
    private String errorMessage;
    private LocalDateTime expiredAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
