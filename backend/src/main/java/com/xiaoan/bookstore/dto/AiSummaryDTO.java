package com.xiaoan.bookstore.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class AiSummaryDTO {

    @NotNull
    private Long bookId;
}
