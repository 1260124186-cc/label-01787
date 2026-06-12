package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.*;
import com.xiaoan.bookstore.entity.SeasonBadge;
import com.xiaoan.bookstore.entity.SeasonParticipant;
import com.xiaoan.bookstore.service.ReadingSeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mp/season")
@RequiredArgsConstructor
public class MpSeasonController {

    private final ReadingSeasonService readingSeasonService;

    @GetMapping("/list")
    public Result<List<SeasonVO>> list(
            @RequestParam(required = false) Integer status) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingSeasonService.listSeasons(status, userId));
    }

    @GetMapping("/{seasonId}")
    public Result<SeasonVO> detail(@PathVariable Long seasonId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingSeasonService.getSeasonDetail(seasonId, userId));
    }

    @PostMapping("/{seasonId}/signup")
    @Log("报名赛季")
    public Result<SeasonParticipant> signup(@PathVariable Long seasonId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingSeasonService.signup(userId, seasonId));
    }

    @PutMapping("/{seasonId}/quit")
    @Log("退出赛季")
    public Result<Void> quit(@PathVariable Long seasonId) {
        Long userId = TenantContext.getTenantId();
        readingSeasonService.quitSeason(userId, seasonId);
        return Result.success();
    }

    @GetMapping("/{seasonId}/progress")
    public Result<SeasonProgressVO> progress(@PathVariable Long seasonId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingSeasonService.getProgress(userId, seasonId));
    }

    @PostMapping("/{seasonId}/sync")
    @Log("同步赛季每日记录")
    public Result<Void> syncDailyRecord(
            @PathVariable Long seasonId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = TenantContext.getTenantId();
        if (date == null) {
            date = LocalDate.now();
        }
        readingSeasonService.syncDailyRecord(userId, seasonId, date);
        return Result.success();
    }

    @GetMapping("/{seasonId}/leaderboard")
    public Result<?> leaderboard(
            @PathVariable Long seasonId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(readingSeasonService.getLeaderboard(seasonId, page, size));
    }

    @GetMapping("/badges")
    public Result<List<SeasonBadgeVO>> badges() {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingSeasonService.getUserBadges(userId));
    }

    @GetMapping("/{seasonId}/badges")
    public Result<List<SeasonBadgeVO>> seasonBadges(@PathVariable Long seasonId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingSeasonService.getSeasonBadges(userId, seasonId));
    }

    @GetMapping("/my")
    public Result<?> mySeasons(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(readingSeasonService.getMySeasonsPaged(userId, status, page, size));
    }
}
