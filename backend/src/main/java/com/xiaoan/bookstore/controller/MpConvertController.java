package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.entity.ConvertTask;
import com.xiaoan.bookstore.service.ConvertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mp/convert")
@RequiredArgsConstructor
public class MpConvertController {

    private final ConvertService convertService;

    @PostMapping("/task")
    @Log("创建转图任务")
    public Result<ConvertTask> createTask(@RequestParam Long bookId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(convertService.createTask(userId, bookId));
    }

    @GetMapping("/task/{taskId}")
    public Result<Map<String, Object>> getTask(@PathVariable Long taskId) {
        Long userId = TenantContext.getTenantId();
        ConvertTask task = convertService.getTask(userId, taskId);
        int position = convertService.getQueuePosition(taskId);

        Map<String, Object> result = new HashMap<>();
        result.put("task", task);
        result.put("queuePosition", position);
        return Result.success(result);
    }

    @GetMapping("/task/latest/{bookId}")
    public Result<ConvertTask> getLatestTask(@PathVariable Long bookId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(convertService.getLatestTask(userId, bookId));
    }

    @GetMapping("/tasks")
    public Result<?> myTasks(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(convertService.myTasks(userId, page, size));
    }
}
