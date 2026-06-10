package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class GroupVO {
    private Long id;
    private String name;
    private String description;
    private String inviteCode;
    private Long creatorId;
    private String creatorNickname;
    private String creatorAvatar;
    private Integer memberCount;
    private Integer status;
    private Integer myRole;
    private Integer myReadingPublic;
    private java.time.LocalDateTime createdAt;
    private List<GroupMemberVO> members;
}
