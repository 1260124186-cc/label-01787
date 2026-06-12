package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("classroom_member")
public class ClassroomMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classroomId;
    private Long userId;
    private Integer role;
    private String studentNo;
    private String realName;
    private LocalDateTime joinedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
