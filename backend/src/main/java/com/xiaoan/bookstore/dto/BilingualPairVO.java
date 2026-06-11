package com.xiaoan.bookstore.dto;

import com.xiaoan.bookstore.entity.BilingualPair;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BilingualPairVO {
    private Long id;
    private Long userId;
    private Long leftBookId;
    private Long rightBookId;
    private String leftBookTitle;
    private String rightBookTitle;
    private String leftBookFormat;
    private String rightBookFormat;
    private String leftLanguage;
    private String rightLanguage;
    private Integer alignmentStrategy;
    private String name;
    private Integer lastLeftUnit;
    private Integer lastRightUnit;
    private Integer leftUnitType;
    private Integer rightUnitType;
    private Integer syncEnabled;
    private Integer aiAlignmentStatus;
    private Integer aiAlignmentProgress;
    private String aiAlignmentError;
    private Integer status;
    private String createdAt;
    private String updatedAt;

    public static BilingualPairVO fromEntity(BilingualPair pair) {
        BilingualPairVO vo = new BilingualPairVO();
        vo.setId(pair.getId());
        vo.setUserId(pair.getUserId());
        vo.setLeftBookId(pair.getLeftBookId());
        vo.setRightBookId(pair.getRightBookId());
        vo.setLeftLanguage(pair.getLeftLanguage());
        vo.setRightLanguage(pair.getRightLanguage());
        vo.setAlignmentStrategy(pair.getAlignmentStrategy());
        vo.setName(pair.getName());
        vo.setLastLeftUnit(pair.getLastLeftUnit());
        vo.setLastRightUnit(pair.getLastRightUnit());
        vo.setLeftUnitType(pair.getLeftUnitType());
        vo.setRightUnitType(pair.getRightUnitType());
        vo.setSyncEnabled(pair.getSyncEnabled());
        vo.setAiAlignmentStatus(pair.getAiAlignmentStatus());
        vo.setAiAlignmentProgress(pair.getAiAlignmentProgress());
        vo.setAiAlignmentError(pair.getAiAlignmentError());
        vo.setStatus(pair.getStatus());
        return vo;
    }
}
