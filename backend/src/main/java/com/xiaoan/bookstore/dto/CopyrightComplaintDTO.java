package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CopyrightComplaintDTO {
    @NotBlank(message = "申诉人姓名不能为空")
    private String complainantName;
    @NotBlank(message = "联系方式不能为空")
    private String complainantContact;
    private Long bookId;
    private String bookTitle;
    @NotBlank(message = "申诉原因不能为空")
    private String reason;
    private String evidenceUrls;
}
