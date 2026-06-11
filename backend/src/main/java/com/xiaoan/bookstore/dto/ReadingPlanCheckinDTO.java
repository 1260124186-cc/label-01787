package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class ReadingPlanCheckinDTO {
    private Long planId;
    private Integer duration;
    private Integer pagesRead;
}
