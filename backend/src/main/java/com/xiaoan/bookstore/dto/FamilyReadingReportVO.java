package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class FamilyReadingReportVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private Long totalDuration;
    private String totalDurationText;
    private Integer bookCount;
    private Integer readingDays;
    private Integer annotationCount;
    private List<Map<String, Object>> bookRank;
    private List<Map<String, Object>> dailyData;
}
