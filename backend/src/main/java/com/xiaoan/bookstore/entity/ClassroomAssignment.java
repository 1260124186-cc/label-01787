package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("classroom_assignment")
public class ClassroomAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classroomId;
    private Long teacherId;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private Integer startPage;
    private Integer endPage;
    private LocalDateTime deadline;
    private String description;
    private Integer totalScore;
    private Integer status;
    private Integer submitCount;
    private Integer gradedCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
