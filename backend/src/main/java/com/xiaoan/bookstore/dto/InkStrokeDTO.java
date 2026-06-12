package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class InkStrokeDTO {
    private Long id;
    @NotNull(message = "书籍ID不能为空")
    private Long bookId;
    @NotNull(message = "页码不能为空")
    private Integer pageNum;
    @NotBlank(message = "笔迹ID不能为空")
    private String strokeId;
    private String strokeType;
    private String color;
    private Double lineWidth;
    private Double opacity;
    @NotBlank(message = "笔迹点数据不能为空")
    private String points;
    private String boundingBox;
}

@Data
class InkStrokeBatchDTO {
    @NotNull(message = "书籍ID不能为空")
    private Long bookId;
    @NotNull(message = "页码不能为空")
    private Integer pageNum;
    private List<InkStrokeDTO> strokes;
    private List<String> deletedStrokeIds;
}

@Data
class InkExportDTO {
    @NotNull(message = "书籍ID不能为空")
    private Long bookId;
    private List<Integer> pageNums;
    private String exportType;
}
