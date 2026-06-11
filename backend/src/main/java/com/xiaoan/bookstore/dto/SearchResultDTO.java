package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class SearchResultDTO {
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookFormat;
    private Integer totalMatches;
    private List<SearchMatch> matches;

    @Data
    public static class SearchMatch {
        private Long id;
        private Integer pageNum;
        private String snippet;
        private String highlightSnippet;
        private Integer matchStart;
        private Integer matchEnd;
        private String sourceType;
        private Long annotationId;
        private String annotationType;
    }
}
