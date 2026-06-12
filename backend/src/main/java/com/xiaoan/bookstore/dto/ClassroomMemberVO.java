package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ClassroomMemberVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer role;
    private String studentNo;
    private String realName;
    private LocalDateTime joinedAt;
}
