package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reading_record")
public class ReadingRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long bookId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private Integer lastPage;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
