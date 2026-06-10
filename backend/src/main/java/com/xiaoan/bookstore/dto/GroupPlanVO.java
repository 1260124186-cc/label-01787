package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class GroupPlanVO {
    private Long id;
    private Long groupId;
    private String bookTitle;
    private String bookAuthor;
    private Long creatorId;
    private String creatorNickname;
    private java.time.LocalDate startDate;
    private java.time.LocalDate endDate;
    private String description;
    private Integer status;
    private java.time.LocalDateTime createdAt;
    private List<GroupPlanMemberVO> members;
}
