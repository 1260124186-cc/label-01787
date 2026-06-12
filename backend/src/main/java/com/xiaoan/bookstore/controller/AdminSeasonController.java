package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.annotation.RequirePermission;
import com.xiaoan.bookstore.annotation.SensitiveOperation;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.SeasonCreateDTO;
import com.xiaoan.bookstore.dto.SeasonStatsVO;
import com.xiaoan.bookstore.dto.SeasonUpdateDTO;
import com.xiaoan.bookstore.dto.SeasonVO;
import com.xiaoan.bookstore.service.ReadingSeasonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/seasons")
@RequiredArgsConstructor
public class AdminSeasonController {

    private final ReadingSeasonService readingSeasonService;

    @GetMapping
    @Log("查看赛季列表")
    @RequirePermission("season:view")
    public Result<List<SeasonVO>> list(
            @RequestParam(required = false) Integer status) {
        return Result.success(readingSeasonService.listSeasons(status, null));
    }

    @GetMapping("/stats")
    @Log("查看赛季统计")
    @RequirePermission("season:view")
    public Result<SeasonStatsVO> stats() {
        return Result.success(readingSeasonService.getAdminStats());
    }

    @GetMapping("/{seasonId}")
    @Log("查看赛季详情")
    @RequirePermission("season:view")
    public Result<Map<String, Object>> detail(@PathVariable Long seasonId) {
        return Result.success(readingSeasonService.getSeasonAdminDetail(seasonId));
    }

    @PostMapping
    @Log("创建赛季")
    @RequirePermission("season:create")
    @SensitiveOperation("create_season")
    public Result<SeasonVO> create(@Valid @RequestBody SeasonCreateDTO dto) {
        return Result.success(readingSeasonService.createSeason(dto));
    }

    @PutMapping("/{seasonId}")
    @Log("编辑赛季")
    @RequirePermission("season:update")
    @SensitiveOperation("update_season")
    public Result<SeasonVO> update(@PathVariable Long seasonId, @RequestBody SeasonUpdateDTO dto) {
        return Result.success(readingSeasonService.updateSeason(seasonId, dto));
    }

    @PutMapping("/{seasonId}/publish")
    @Log("发布赛季")
    @RequirePermission("season:update")
    @SensitiveOperation("publish_season")
    public Result<Void> publish(@PathVariable Long seasonId) {
        readingSeasonService.publishSeason(seasonId);
        return Result.success();
    }

    @DeleteMapping("/{seasonId}")
    @Log("删除赛季")
    @RequirePermission("season:delete")
    @SensitiveOperation("delete_season")
    public Result<Void> delete(@PathVariable Long seasonId) {
        readingSeasonService.deleteSeason(seasonId);
        return Result.success();
    }

    @PutMapping("/{seasonId}/end")
    @Log("手动结束赛季")
    @RequirePermission("season:update")
    @SensitiveOperation("end_season")
    public Result<Void> endSeason(@PathVariable Long seasonId) {
        readingSeasonService.endSeason(seasonId);
        return Result.success();
    }

    @PostMapping("/{seasonId}/sync")
    @Log("同步赛季数据")
    @RequirePermission("season:update")
    public Result<Void> syncSeasonData(@PathVariable Long seasonId) {
        readingSeasonService.syncAllParticipantDailyRecords(seasonId);
        return Result.success();
    }

    @PostMapping("/{seasonId}/award-prizes")
    @Log("发放赛季奖品")
    @RequirePermission("season:prize")
    @SensitiveOperation("award_season_prizes")
    public Result<Void> awardPrizes(@PathVariable Long seasonId) {
        readingSeasonService.awardPrizes(seasonId);
        return Result.success();
    }

    @GetMapping("/cheat-detections")
    @Log("查看作弊检测列表")
    @RequirePermission("season:cheat")
    public Result<?> cheatDetections(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(readingSeasonService.listCheatDetections(status, page, size));
    }

    @PutMapping("/cheat-detections/{detectionId}")
    @Log("处理作弊检测")
    @RequirePermission("season:cheat")
    @SensitiveOperation("handle_cheat_detection")
    public Result<Void> handleCheatDetection(
            @PathVariable Long detectionId,
            @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        String handleResult = body.get("handleResult") != null ? String.valueOf(body.get("handleResult")) : "";
        if (status == null) {
            return Result.error(400, "处理状态不能为空");
        }
        Long handlerId = TenantContext.getTenantId();
        readingSeasonService.handleCheatDetection(detectionId, handlerId, status, handleResult);
        return Result.success();
    }
}
