package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.annotation.RequirePermission;
import com.xiaoan.bookstore.annotation.SensitiveOperation;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.entity.BookGroup;
import com.xiaoan.bookstore.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/groups")
@RequiredArgsConstructor
public class AdminGroupController {

    private final GroupService groupService;

    @GetMapping
    @Log("查看小组列表")
    @RequirePermission("group:view")
    public Result<IPage<BookGroup>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(groupService.adminGroupList(page, size, keyword, status));
    }

    @PutMapping("/{id}/ban")
    @Log("封禁小组")
    @RequirePermission("group:ban")
    @SensitiveOperation("ban_group")
    public Result<Void> ban(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long adminId = TenantContext.getTenantId();
        String reason = body.get("reason");
        if (reason == null || reason.isEmpty()) {
            return Result.error(400, "封禁原因不能为空");
        }
        groupService.banGroup(adminId, id, reason);
        return Result.success();
    }

    @PutMapping("/{id}/unban")
    @Log("解封小组")
    @RequirePermission("group:ban")
    public Result<Void> unban(@PathVariable Long id) {
        Long adminId = TenantContext.getTenantId();
        groupService.unbanGroup(adminId, id);
        return Result.success();
    }

    @GetMapping("/{id}/dynamics")
    @Log("查看小组动态")
    @RequirePermission("group:dynamic_view")
    public Result<IPage<Map<String, Object>>> dynamics(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(groupService.adminDynamicList(id, page, size));
    }
}
