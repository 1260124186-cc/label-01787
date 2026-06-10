package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PointsExchangeDTO {

    @NotNull
    private Integer exchangeType;

    @NotNull
    private Integer value;
}
