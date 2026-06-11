package com.xiaoan.bookstore.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class AiChatDTO {

    @NotNull
    private Long bookId;

    private Integer type;

    private Integer sourceType;

    private String sourceText;

    private Integer pageNum;

    private String userPrompt;

    private String targetLanguage;
}
