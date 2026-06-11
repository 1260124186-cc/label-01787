package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.AiChatDTO;
import com.xiaoan.bookstore.dto.AiSummaryDTO;
import com.xiaoan.bookstore.entity.AiChatHistory;
import com.xiaoan.bookstore.entity.AiSummary;
import com.xiaoan.bookstore.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mp/ai")
@RequiredArgsConstructor
public class MpAiController {

    private final AiService aiService;

    @PostMapping("/chat")
    @Log("AI对话处理")
    public Result<AiChatHistory> chat(@Valid @RequestBody AiChatDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(aiService.processChat(userId, dto));
    }

    @GetMapping("/chat/sessions")
    public Result<List<Map<String, Object>>> getChatSessions() {
        Long userId = TenantContext.getTenantId();
        return Result.success(aiService.getBookChatSessions(userId));
    }

    @GetMapping("/chat/book/{bookId}")
    public Result<Page<AiChatHistory>> getChatHistoryByBook(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(aiService.getChatHistoryByBook(userId, bookId, page, size));
    }

    @GetMapping("/chat/{id}")
    public Result<AiChatHistory> getChatDetail(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        return Result.success(aiService.getChatDetail(userId, id));
    }

    @DeleteMapping("/chat/{id}")
    @Log("删除AI对话记录")
    public Result<Void> deleteChat(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        aiService.deleteChat(userId, id);
        return Result.success();
    }

    @DeleteMapping("/chat/book/{bookId}")
    @Log("清空书籍AI对话记录")
    public Result<Void> clearBookChats(@PathVariable Long bookId) {
        Long userId = TenantContext.getTenantId();
        aiService.clearBookChats(userId, bookId);
        return Result.success();
    }

    @GetMapping("/disclaimer")
    public Result<Map<String, Object>> getDisclaimer() {
        return Result.success(aiService.getDisclaimerInfo());
    }

    @PostMapping("/copyright/agree/{bookId}")
    @Log("确认书籍版权声明")
    public Result<Void> agreeCopyright(@PathVariable Long bookId) {
        Long userId = TenantContext.getTenantId();
        aiService.agreeCopyright(userId, bookId);
        return Result.success();
    }

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
    public Result<Page<AiSummary>> mySummaries(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(aiService.mySummaries(userId, page, size));
    }
}
