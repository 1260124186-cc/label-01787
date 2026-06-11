package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.entity.Family;
import com.xiaoan.bookstore.service.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/families")
@RequiredArgsConstructor
public class AdminFamilyController {

    private final FamilyService familyService;

    @GetMapping
    public Result<IPage<Family>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String keyword) {
        return Result.success(familyService.adminFamilyList(page, size, keyword));
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(familyService.adminDashboard());
    }
}
