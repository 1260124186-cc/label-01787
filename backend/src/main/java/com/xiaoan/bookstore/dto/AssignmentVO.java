package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AssignmentVO {
    private Long id;
    private Long classroomId;
    private Long teacherId;
    private String teacherNickname;
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
    private Integer totalMembers;
    private Double avgScore;
    private LocalDateTime createdAt;
}
