package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class BilingualPairUpdateDTO {
    private String leftLanguage;
    private String rightLanguage;
    private Integer alignmentStrategy;
    private String name;
    private Integer syncEnabled;
    private Integer lastLeftUnit;
    private Integer lastRightUnit;
}
