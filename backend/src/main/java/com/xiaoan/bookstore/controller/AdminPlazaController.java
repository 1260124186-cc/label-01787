package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.ExcerptAuditDTO;
import com.xiaoan.bookstore.entity.ExcerptReport;
import com.xiaoan.bookstore.entity.PublicExcerpt;
import com.xiaoan.bookstore.service.PlazaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/plaza")
@RequiredArgsConstructor
public class AdminPlazaController {

    private final PlazaService plazaService;

    @GetMapping("/excerpts")
    public Result<IPage<PublicExcerpt>> listExcerpts(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(plazaService.adminListExcerpts(status, auditStatus, page, size));
    }

    @PostMapping("/excerpts/{id}/audit")
    @Log("审核书摘")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody ExcerptAuditDTO dto) {
        Long auditorId = TenantContext.getTenantId();
        plazaService.auditExcerpt(id, dto.getAuditStatus(), dto.getReason(), auditorId);
        return Result.success();
    }

    @PostMapping("/excerpts/{id}/remove")
    @Log("下架书摘")
    public Result<Void> remove(@PathVariable Long id) {
        Long handlerId = TenantContext.getTenantId();
        plazaService.removeExcerpt(id, handlerId);
        return Result.success();
    }

    @GetMapping("/reports")
    public Result<IPage<ExcerptReport>> listReports(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(plazaService.adminListReports(status, page, size));
    }

    @PostMapping("/reports/{id}/handle")
    @Log("处理书摘举报")
    public Result<Void> handleReport(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String handleResult) {
        Long handlerId = TenantContext.getTenantId();
        plazaService.handleReport(id, status, handleResult, handlerId);
        return Result.success();
    }
}
