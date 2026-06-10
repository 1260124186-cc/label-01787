package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.entity.ConvertTask;
import com.xiaoan.bookstore.service.ConvertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/convert")
@RequiredArgsConstructor
public class AdminConvertController {

    private final ConvertService convertService;

    @GetMapping("/tasks")
    public Result<?> taskList(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(required = false) Integer status) {
        return Result.success(convertService.adminList(page, size, status));
    }

    @GetMapping("/task/{id}")
    public Result<ConvertTask> getTask(@PathVariable Long id) {
        return Result.success(convertService.getTaskById(id));
    }
}
