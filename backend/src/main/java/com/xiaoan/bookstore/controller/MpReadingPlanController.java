package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.ReadingPlanCheckinDTO;
import com.xiaoan.bookstore.dto.ReadingPlanCreateDTO;
import com.xiaoan.bookstore.dto.ReadingPlanProgressVO;
import com.xiaoan.bookstore.dto.ReadingPlanVO;
import com.xiaoan.bookstore.entity.ReadingPlanBadge;
import com.xiaoan.bookstore.entity.ReadingPlanCheckin;
import com.xiaoan.bookstore.service.ReadingPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mp/reading-plan")
@RequiredArgsConstructor
public class MpReadingPlanController {

    private final ReadingPlanService readingPlanService;

    @PostMapping("/create")
    @Log("创建阅读计划")
    public Result<ReadingPlanVO> create(@Valid @RequestBody ReadingPlanCreateDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingPlanService.createPlan(userId, dto));
    }

    @GetMapping("/list")
    public Result<List<ReadingPlanVO>> list(
            @RequestParam(required = false) Integer status) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingPlanService.listPlans(userId, status));
    }

    @GetMapping("/{planId}/progress")
    public Result<ReadingPlanProgressVO> progress(@PathVariable Long planId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingPlanService.getProgress(userId, planId));
    }

    @PostMapping("/checkin")
    @Log("阅读计划打卡")
    public Result<ReadingPlanCheckin> checkin(@Valid @RequestBody ReadingPlanCheckinDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingPlanService.checkin(userId, dto));
    }

    @GetMapping("/{planId}/calendar")
    public Result<List<String>> calendar(@PathVariable Long planId) {
        Long userId = TenantContext.getTenantId();
        List<LocalDate> dates = readingPlanService.getCheckinCalendar(userId, planId);
        List<String> dateStrings = dates.stream().map(LocalDate::toString).toList();
        return Result.success(dateStrings);
    }

    @GetMapping("/badges")
    public Result<List<ReadingPlanBadge>> badges() {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingPlanService.listBadges(userId));
    }

    @PutMapping("/{planId}/abandon")
    @Log("放弃阅读计划")
    public Result<Void> abandon(@PathVariable Long planId) {
        Long userId = TenantContext.getTenantId();
        readingPlanService.abandonPlan(userId, planId);
        return Result.success();
    }
}
