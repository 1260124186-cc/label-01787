package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class PointsVO {

    private Integer balance;

    private Integer totalEarned;

    private Integer totalConsumed;

    private Boolean todayCheckedIn;
}
