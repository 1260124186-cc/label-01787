package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class SearchRequestDTO {
    private String keyword;
    private String scope;
    private Long bookId;
    private Integer page;
    private Integer size;
}
