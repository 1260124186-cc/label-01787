package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.ReadingDTO;
import com.xiaoan.bookstore.dto.ReadingGoalDTO;
import com.xiaoan.bookstore.dto.ReadingGoalProgressVO;
import com.xiaoan.bookstore.dto.ReadingReportVO;
import com.xiaoan.bookstore.dto.ReadingSummaryVO;
import com.xiaoan.bookstore.entity.ReadingGoal;
import com.xiaoan.bookstore.entity.ReadingRecord;
import com.xiaoan.bookstore.service.ReadingGoalService;
import com.xiaoan.bookstore.service.ReadingReportService;
import com.xiaoan.bookstore.service.ReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mp/reading")
@RequiredArgsConstructor
public class MpReadingController {

    private final ReadingService readingService;
    private final ReadingGoalService readingGoalService;
    private final ReadingReportService readingReportService;

    @PostMapping("/start")
    @Log("开始阅读")
    public Result<ReadingRecord> start(@Valid @RequestBody ReadingDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingService.startReading(userId, dto.getBookId()));
    }

    @PostMapping("/end")
    @Log("结束阅读")
    public Result<Void> end(@Valid @RequestBody ReadingDTO dto) {
        Long userId = TenantContext.getTenantId();
        readingService.endReading(userId, dto.getRecordId(), dto.getLastPage());
        return Result.success();
    }

    @GetMapping("/summary")
    public Result<ReadingSummaryVO> summary(@RequestParam(defaultValue = "week") String period) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingService.summary(userId, period));
    }

    @GetMapping("/continue-list")
    public Result<List<Map<String, Object>>> continueReadingList(
            @RequestParam(defaultValue = "5") Integer limit) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingService.continueReadingList(userId, limit));
    }

    @GetMapping("/timeline")
    public Result<List<Map<String, Object>>> timeline(
            @RequestParam(defaultValue = "month") String period) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingService.readingTimeline(userId, period));
    }

    @GetMapping("/goal")
    public Result<ReadingGoal> getGoal() {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingGoalService.getGoal(userId));
    }

    @PutMapping("/goal")
    @Log("更新阅读目标")
    public Result<ReadingGoal> updateGoal(@RequestBody ReadingGoalDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingGoalService.updateGoal(userId, dto));
    }

    @GetMapping("/goal/progress")
    public Result<ReadingGoalProgressVO> getGoalProgress() {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingGoalService.getProgress(userId));
    }

    @GetMapping("/report")
    @Log("生成阅读报告")
    public Result<ReadingReportVO> generateReport(@RequestParam(defaultValue = "week") String period) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingReportService.generateReport(userId, period));
    }

    @GetMapping("/report/list")
    public Result<List<?>> getReportList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingReportService.getReportList(userId, page, size));
    }
}
