package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("classroom")
public class Classroom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Long teacherId;
    private String inviteCode;
    private Integer memberCount;
    private String institution;
    private String gradeLevel;
    private Integer status;
    private String banReason;
    private LocalDateTime bannedAt;
    private Long bannedBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
