package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("classroom_reminder")
public class ClassroomReminder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assignmentId;
    private Long teacherId;
    private Long studentId;
    private String message;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
