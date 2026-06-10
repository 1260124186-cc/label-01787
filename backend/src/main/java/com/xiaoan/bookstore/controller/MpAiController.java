package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.AiSummaryDTO;
import com.xiaoan.bookstore.entity.AiSummary;
import com.xiaoan.bookstore.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/mp/ai")
@RequiredArgsConstructor
public class MpAiController {

    private final AiService aiService;

    @PostMapping("/summary")
    @Log("生成AI摘要")
    public Result<AiSummary> generateSummary(@Valid @RequestBody AiSummaryDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(aiService.generateSummary(userId, dto));
    }

    @GetMapping("/summary/{bookId}")
    public Result<AiSummary> getSummary(@PathVariable Long bookId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(aiService.getLatestSummary(userId, bookId));
    }

    @GetMapping("/summaries")
    public Result<?> mySummaries(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(aiService.mySummaries(userId, page, size));
    }
}
