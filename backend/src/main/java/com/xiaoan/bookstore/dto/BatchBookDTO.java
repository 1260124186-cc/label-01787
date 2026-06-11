package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchBookDTO {
    private List<Long> bookIds;
    private Long categoryId;
}
