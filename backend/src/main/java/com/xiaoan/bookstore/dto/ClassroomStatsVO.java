package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class ClassroomStatsVO {
    private Long classroomId;
    private String classroomName;
    private Integer totalStudents;
    private Integer totalAssignments;
    private Integer activeAssignments;
    private Double overallAvgScore;
    private Double overallSubmitRate;
    private List<AssignmentStatsItem> assignmentStats;

    @Data
    public static class AssignmentStatsItem {
        private Long assignmentId;
        private String bookTitle;
        private Integer submitCount;
        private Integer totalMembers;
        private Double submitRate;
        private Double avgScore;
        private Integer gradedCount;
    }
}
