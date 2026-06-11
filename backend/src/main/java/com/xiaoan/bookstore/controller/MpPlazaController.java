package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.ExcerptPublishDTO;
import com.xiaoan.bookstore.dto.ExcerptReportDTO;
import com.xiaoan.bookstore.dto.PublicExcerptVO;
import com.xiaoan.bookstore.entity.PublicExcerpt;
import com.xiaoan.bookstore.service.PlazaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mp/plaza")
@RequiredArgsConstructor
public class MpPlazaController {

    private final PlazaService plazaService;

    @PostMapping("/publish")
    @Log("发布书摘到广场")
    public Result<PublicExcerpt> publish(@Valid @RequestBody ExcerptPublishDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(plazaService.publishExcerpt(userId, dto));
    }

    @PostMapping("/{id}/withdraw")
    @Log("撤回书摘")
    public Result<Void> withdraw(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        plazaService.withdrawExcerpt(userId, id);
        return Result.success();
    }

    @GetMapping("/excerpts")
    public Result<IPage<PublicExcerptVO>> listExcerpts(
            @RequestParam(defaultValue = "latest") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(plazaService.listExcerpts(userId, sortBy, page, size));
    }

    @GetMapping("/excerpts/{id}")
    public Result<PublicExcerptVO> getDetail(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        return Result.success(plazaService.getExcerptDetail(userId, id));
    }

    @GetMapping("/my/excerpts")
    public Result<IPage<PublicExcerptVO>> myExcerpts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(plazaService.myExcerpts(userId, page, size));
    }

    @GetMapping("/my/favorites")
    public Result<IPage<PublicExcerptVO>> myFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(plazaService.myFavorites(userId, page, size));
    }

    @PostMapping("/excerpts/{id}/like")
    @Log("点赞书摘")
    public Result<Map<String, Boolean>> toggleLike(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        boolean liked = plazaService.toggleLike(userId, id);
        return Result.success(Map.of("liked", liked));
    }

    @PostMapping("/excerpts/{id}/favorite")
    @Log("收藏书摘")
    public Result<Map<String, Boolean>> toggleFavorite(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        boolean favorited = plazaService.toggleFavorite(userId, id);
        return Result.success(Map.of("favorited", favorited));
    }

    @PostMapping("/report")
    @Log("举报书摘")
    public Result<Void> report(@Valid @RequestBody ExcerptReportDTO dto) {
        Long userId = TenantContext.getTenantId();
        plazaService.reportExcerpt(userId, dto);
        return Result.success();
    }
}
