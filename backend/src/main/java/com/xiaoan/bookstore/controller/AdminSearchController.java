package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.dto.IndexStatusVO;
import com.xiaoan.bookstore.service.BookIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
public class AdminSearchController {

    private final BookIndexService bookIndexService;

    @GetMapping("/index-status")
    public Result<Page<IndexStatusVO>> getIndexStatus(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status) {
        return Result.success(bookIndexService.getIndexStatus(page, size, status));
    }

    @GetMapping("/index-status/book/{bookId}")
    public Result<IndexStatusVO> getBookIndexStatus(@PathVariable Long bookId) {
        return Result.success(bookIndexService.getBookIndexStatus(bookId));
    }

    @GetMapping("/alerts")
    public Result<List<IndexStatusVO>> getFailedAlerts() {
        return Result.success(bookIndexService.getFailedAlerts());
    }

    @PostMapping("/rebuild/{bookId}")
    @Log("重建书籍索引")
    public Result<Void> rebuildIndex(@PathVariable Long bookId) {
        bookIndexService.rebuildIndex(bookId);
        return Result.success();
    }

    @PostMapping("/rebuild-all")
    @Log("批量重建索引")
    public Result<Void> rebuildAll() {
        bookIndexService.rebuildAllPending();
        return Result.success();
    }

    @PostMapping("/retry/{taskId}")
    @Log("重试索引任务")
    public Result<Void> retryTask(@PathVariable Long taskId) {
        bookIndexService.retryTask(taskId);
        return Result.success();
    }
}
