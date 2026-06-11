package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FamilyMemberVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer role;
    private String familyNickname;
    private LocalDateTime joinedAt;
}
