package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FamilyVO {
    private Long id;
    private String name;
    private String inviteCode;
    private Long ownerId;
    private String ownerNickname;
    private String ownerAvatar;
    private Integer memberCount;
    private Integer maxMembers;
    private Long sharedStorage;
    private Integer status;
    private Integer myRole;
    private LocalDateTime createdAt;
    private List<FamilyMemberVO> members;
}
