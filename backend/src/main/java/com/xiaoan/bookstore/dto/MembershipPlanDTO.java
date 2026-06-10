package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MembershipPlanDTO {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Integer price;

    @NotNull
    private Integer durationDays;

    @NotNull
    private Integer maxBooks;

    @NotNull
    private Long maxStorage;

    @NotNull
    private Integer aiDailyLimit;

    private Integer priorityQueue;

    private Integer advancedStats;

    private Integer sortOrder;
}
