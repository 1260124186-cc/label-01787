package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("focus_session")
public class FocusSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer duration;
    private Integer actualDuration;
    private Integer status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer pomodoroIndex;
    private String tag;
    private String note;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
