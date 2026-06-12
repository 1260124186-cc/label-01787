package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class SeasonDailyRecordVO {
    private String date;
    private Integer duration;
    private Integer bookCount;
    private Boolean isQualified;
    private Boolean isFlagged;
    private String flagReason;
    private String formattedDuration;
}
