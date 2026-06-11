package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class ReadingHistoryVO {
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookFormat;
    private String coverThumbnail;
    private Integer lastPage;
    private Integer lastChapter;
    private Integer pageCount;
    private Integer chapterCount;
    private Integer sessionDuration;
    private Long totalDuration;
    private String lastReadTime;
    private Integer readCount;
    private Double progress;
}
