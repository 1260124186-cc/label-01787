package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExportDataDTO {

    @Data
    public static class CategoryExport {
        private Long id;
        private String name;
        private Integer sortOrder;
        private LocalDateTime createdAt;
    }

    @Data
    public static class BookExport {
        private Long id;
        private String title;
        private String author;
        private String originalFileName;
        private Long fileSize;
        private Integer pageCount;
        private Long categoryId;
        private Integer lastPage;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class AnnotationExport {
        private Long id;
        private Long bookId;
        private String bookTitle;
        private Integer pageNum;
        private String selectedText;
        private String content;
        private Integer type;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class ReadingRecordExport {
        private Long id;
        private Long bookId;
        private String bookTitle;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer duration;
        private Integer lastPage;
        private LocalDateTime createdAt;
    }

    @Data
    public static class ImportResult {
        private int categoryImported;
        private int categorySkipped;
        private int annotationImported;
        private int annotationSkipped;
        private int recordImported;
        private int recordSkipped;
        private List<BookImportItem> booksToLink;
        private List<String> warnings;
    }

    @Data
    public static class BookImportItem {
        private String originalBookId;
        private String title;
        private String author;
        private Integer annotationCount;
        private Integer recordCount;
    }
}
