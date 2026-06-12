package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClassroomVO {
    private Long id;
    private String name;
    private String description;
    private Long teacherId;
    private String teacherNickname;
    private String teacherAvatar;
    private String inviteCode;
    private Integer memberCount;
    private String institution;
    private String gradeLevel;
    private Integer status;
    private Integer myRole;
    private LocalDateTime createdAt;
    private List<ClassroomMemberVO> members;
}
