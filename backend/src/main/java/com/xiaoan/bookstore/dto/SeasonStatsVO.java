package com.xiaoan.bookstore.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SeasonStatsVO {
    private Long totalSeasons;
    private Long activeSeasons;
    private Long totalParticipants;
    private List<Map<String, Object>> seasonTypeStats;
    private List<Map<String, Object>> dailyCreationStats;
}
