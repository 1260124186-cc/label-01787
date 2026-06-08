package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class StorageStatsVO {
    private Long totalUsers;
    private Long totalBooks;
    private Long totalFileSize;
    private String totalFileSizeText;
    private Long totalAnnotations;
    private Long totalReadingRecords;
    private Long totalCategories;
    private List<UserStorageStats> topUsers;
    private List<DailyStorageStats> dailyTrend;

    @Data
    public static class UserStorageStats {
        private Long userId;
        private String nickname;
        private Long bookCount;
        private Long fileSize;
        private String fileSizeText;
        private Double percentage;
    }

    @Data
    public static class DailyStorageStats {
        private String date;
        private Long fileSize;
        private Long bookCount;
        private Long userCount;
    }
}
