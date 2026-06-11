package com.xiaoan.bookstore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_chat_history")
public class AiChatHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookId;

    private String bookTitle;

    private String sessionId;

    private Integer type;

    private Integer sourceType;

    private String sourceText;

    private Integer pageNum;

    private String userPrompt;

    private String aiResponse;

    private String extraData;

    private Integer status;

    private String errorMsg;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public interface Type {
        int SUMMARY = 1;
        int EXPLAIN = 2;
        int TRANSLATE = 3;
        int QUIZ = 4;
        int OUTLINE = 5;
        int KNOWLEDGE_CARD = 6;
    }

    public interface SourceType {
        int SELECTED_TEXT = 1;
        int CURRENT_PAGE = 2;
        int WHOLE_BOOK = 3;
    }

    public interface Status {
        int FAILED = 0;
        int SUCCESS = 1;
        int GENERATING = 2;
    }
}
