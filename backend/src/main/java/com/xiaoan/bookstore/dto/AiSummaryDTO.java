package com.xiaoan.bookstore.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class AiSummaryDTO {

    @NotNull
    private Long bookId;
}
