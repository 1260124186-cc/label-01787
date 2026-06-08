package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BackupManifest {
    private String version;
    private LocalDateTime exportedAt;
    private String source;
    private BackupStats stats;
    private String schemaVersion;

    @Data
    public static class BackupStats {
        private int bookCount;
        private int annotationCount;
        private int recordCount;
        private int categoryCount;
        private long totalFileSize;
    }
}
