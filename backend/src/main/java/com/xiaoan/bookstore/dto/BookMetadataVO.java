package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class BookMetadataVO {
    private String title;
    private String author;
    private String isbn;
    private String coverUrl;
    private String description;
    private Double rating;
    private Integer ratingCount;
    private List<String> tags;
    private String publisher;
    private String publishDate;
    private String language;
    private Integer pageCount;
    private String source;
    private String sourceId;
}
