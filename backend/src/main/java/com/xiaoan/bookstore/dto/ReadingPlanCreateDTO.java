package com.xiaoan.bookstore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReadingPlanCreateDTO {
    @NotNull(message = "书籍ID不能为空")
    private Long bookId;

    @NotNull(message = "目标天数不能为空")
    @Min(value = 1, message = "目标天数最少1天")
    @Max(value = 365, message = "目标天数最多365天")
    private Integer targetDays;

    @Min(value = 60, message = "每日最低时长不少于60秒")
    @Max(value = 86400, message = "每日最低时长不超过86400秒")
    private Integer dailyMinDuration;

    private String reminderTime;
}
