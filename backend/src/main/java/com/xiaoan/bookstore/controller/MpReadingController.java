package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.dto.ReadingDTO;
import com.xiaoan.bookstore.dto.ReadingSummaryVO;
import com.xiaoan.bookstore.entity.ReadingRecord;
import com.xiaoan.bookstore.service.ReadingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mp/reading")
@RequiredArgsConstructor
public class MpReadingController {

    private final ReadingService readingService;

    @PostMapping("/start")
    @Log("开始阅读")
    public Result<ReadingRecord> start(HttpServletRequest request, @Valid @RequestBody ReadingDTO dto) {
        Long userId = (Long) request.getAttribute(Constants.CONTEXT_USER_ID);
        return Result.success(readingService.startReading(userId, dto.getBookId()));
    }

    @PostMapping("/end")
    @Log("结束阅读")
    public Result<Void> end(HttpServletRequest request, @Valid @RequestBody ReadingDTO dto) {
        Long userId = (Long) request.getAttribute(Constants.CONTEXT_USER_ID);
        readingService.endReading(userId, dto.getRecordId(), dto.getLastPage());
        return Result.success();
    }

    @GetMapping("/summary")
    public Result<ReadingSummaryVO> summary(HttpServletRequest request,
                                             @RequestParam(defaultValue = "week") String period) {
        Long userId = (Long) request.getAttribute(Constants.CONTEXT_USER_ID);
        return Result.success(readingService.summary(userId, period));
    }
}
