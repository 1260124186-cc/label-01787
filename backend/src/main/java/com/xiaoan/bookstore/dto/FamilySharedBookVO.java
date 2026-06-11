package com.xiaoan.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FamilySharedBookVO {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookFormat;
    private String coverThumbnail;
    private Long sharedBy;
    private String sharedByNickname;
    private LocalDateTime sharedAt;
}
