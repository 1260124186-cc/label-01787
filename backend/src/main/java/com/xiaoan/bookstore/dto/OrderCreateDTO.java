package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCreateDTO {

    @NotNull
    private Long planId;

    @NotNull
    private Integer orderType;

    private Integer storageGB;
}
