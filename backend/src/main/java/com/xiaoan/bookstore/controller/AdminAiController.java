package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.entity.AiSummary;
import com.xiaoan.bookstore.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiService aiService;

    @GetMapping("/summaries")
    public Result<?> summaryList(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) Long userId) {
        return Result.success(aiService.adminList(page, size, userId));
    }

    @GetMapping("/summary/{id}")
    public Result<AiSummary> getSummary(@PathVariable Long id) {
        return Result.success(aiService.getById(id));
    }
}
