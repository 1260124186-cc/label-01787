package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BookUpdateDTO {
    @Size(max = 200, message = "书名不能超过200字")
    private String title;

    @Size(max = 100, message = "作者名不能超过100字")
    private String author;

    private Long categoryId;
}
