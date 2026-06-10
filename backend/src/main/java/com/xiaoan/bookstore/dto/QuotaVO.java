package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class QuotaVO {

    private String planCode;

    private String planName;

    private Integer maxBooks;

    private Integer currentBooks;

    private Long maxStorage;

    private Long usedStorage;

    private Integer aiDailyLimit;

    private Integer aiUsedToday;

    private Boolean priorityQueue;

    private Boolean advancedStats;

    private Boolean isVip;

    private String expireAt;
}
