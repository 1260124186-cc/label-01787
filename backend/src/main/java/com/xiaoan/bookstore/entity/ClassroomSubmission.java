package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("classroom_submission")
public class ClassroomSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private Integer readingDuration;
    private String annotationSummary;
    private Integer pageProgress;
    private String proofImages;
    private LocalDateTime submitAt;
    private Integer status;
    private Integer score;
    private String teacherComment;
    private LocalDateTime gradedAt;
    private Long gradedBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
