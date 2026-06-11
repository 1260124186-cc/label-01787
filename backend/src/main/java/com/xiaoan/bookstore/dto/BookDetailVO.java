package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class BookDetailVO {
    private Long id;
    private Long userId;
    private String title;
    private String author;
    private String filePath;
    private Long fileSize;
    private String fileSizeText;
    private String bookFormat;
    private Integer pageCount;
    private Integer chapterCount;
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private Integer lastPage;
    private Integer lastChapter;
    private Integer copyrightDeclared;
    private Integer status;
    private Integer progressPercent;
    private Integer annotationCount;
    private Long totalDuration;
    private String totalDurationText;
    private String createdAt;
    private String updatedAt;
    private String lastReadAt;

    private String isbn;
    private String coverUrl;
    private String description;
    private Double rating;
    private Integer ratingCount;
    private List<String> tags;
    private String publisher;
    private String publishDate;
    private String language;
    private String metadataSource;
    private String metadataFetchedAt;
}
