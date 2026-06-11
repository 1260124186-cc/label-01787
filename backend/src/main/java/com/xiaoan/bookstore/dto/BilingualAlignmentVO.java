package com.xiaoan.bookstore.dto;

import com.xiaoan.bookstore.entity.BilingualAlignment;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BilingualAlignmentVO {
    private Long id;
    private Long pairId;
    private Integer leftUnitIndex;
    private Integer rightUnitIndex;
    private Integer leftParagraphIndex;
    private Integer rightParagraphIndex;
    private Integer alignmentMethod;
    private BigDecimal confidence;
    private String leftTextSnippet;
    private String rightTextSnippet;
    private Integer sortOrder;

    public static BilingualAlignmentVO fromEntity(BilingualAlignment a) {
        BilingualAlignmentVO vo = new BilingualAlignmentVO();
        vo.setId(a.getId());
        vo.setPairId(a.getPairId());
        vo.setLeftUnitIndex(a.getLeftUnitIndex());
        vo.setRightUnitIndex(a.getRightUnitIndex());
        vo.setLeftParagraphIndex(a.getLeftParagraphIndex());
        vo.setRightParagraphIndex(a.getRightParagraphIndex());
        vo.setAlignmentMethod(a.getAlignmentMethod());
        vo.setConfidence(a.getConfidence());
        vo.setLeftTextSnippet(a.getLeftTextSnippet());
        vo.setRightTextSnippet(a.getRightTextSnippet());
        vo.setSortOrder(a.getSortOrder());
        return vo;
    }
}
