package com.xiaoan.bookstore.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class BilingualPairCreateDTO {
    @NotNull(message = "左侧书籍ID不能为空")
    private Long leftBookId;
    @NotNull(message = "右侧书籍ID不能为空")
    private Long rightBookId;
    private String leftLanguage;
    private String rightLanguage;
    private Integer alignmentStrategy;
    private String name;
}
