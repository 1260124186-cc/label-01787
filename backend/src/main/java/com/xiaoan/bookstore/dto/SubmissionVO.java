package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SubmissionVO {
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String studentNickname;
    private String studentAvatar;
    private Integer readingDuration;
    private String annotationSummary;
    private Integer pageProgress;
    private String proofImages;
    private LocalDateTime submitAt;
    private Integer status;
    private Integer score;
    private String teacherComment;
    private LocalDateTime gradedAt;
    private String gradedByName;
}
