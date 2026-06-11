package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.*;
import com.xiaoan.bookstore.entity.Family;
import com.xiaoan.bookstore.entity.FamilyMember;
import com.xiaoan.bookstore.entity.FamilySharedBook;
import com.xiaoan.bookstore.service.FamilyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mp/families")
@RequiredArgsConstructor
public class MpFamilyController {

    private final FamilyService familyService;

    @PostMapping
    @Log("创建家庭")
    public Result<Family> create(@Valid @RequestBody FamilyCreateDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(familyService.createFamily(userId, dto));
    }

    @PostMapping("/join")
    @Log("加入家庭")
    public Result<FamilyMember> join(@Valid @RequestBody FamilyJoinDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(familyService.joinFamily(userId, dto));
    }

    @PostMapping("/{familyId}/leave")
    @Log("退出家庭")
    public Result<Void> leave(@PathVariable Long familyId) {
        Long userId = TenantContext.getTenantId();
        familyService.leaveFamily(userId, familyId);
        return Result.success();
    }

    @PostMapping("/{familyId}/dissolve")
    @Log("解散家庭")
    public Result<Void> dissolve(@PathVariable Long familyId) {
        Long userId = TenantContext.getTenantId();
        familyService.dissolveFamily(userId, familyId);
        return Result.success();
    }

    @DeleteMapping("/{familyId}/members/{targetUserId}")
    @Log("移除家庭成员")
    public Result<Void> removeMember(@PathVariable Long familyId, @PathVariable Long targetUserId) {
        Long userId = TenantContext.getTenantId();
        familyService.removeMember(userId, familyId, targetUserId);
        return Result.success();
    }

    @GetMapping("/my")
    public Result<List<Family>> myFamilies() {
        Long userId = TenantContext.getTenantId();
        return Result.success(familyService.myFamilies(userId));
    }

    @GetMapping("/{familyId}")
    public Result<FamilyVO> detail(@PathVariable Long familyId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(familyService.getFamilyDetail(userId, familyId));
    }

    @PostMapping("/{familyId}/books/share")
    @Log("共享书籍到家庭书架")
    public Result<FamilySharedBook> shareBook(@PathVariable Long familyId,
                                               @Valid @RequestBody FamilySharedBookDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(familyService.shareBook(userId, familyId, dto));
    }

    @DeleteMapping("/{familyId}/books/{bookId}")
    @Log("取消家庭书架共享")
    public Result<Void> unshareBook(@PathVariable Long familyId, @PathVariable Long bookId) {
        Long userId = TenantContext.getTenantId();
        familyService.unshareBook(userId, familyId, bookId);
        return Result.success();
    }

    @GetMapping("/{familyId}/books")
    public Result<List<FamilySharedBookVO>> getSharedBooks(@PathVariable Long familyId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(familyService.getSharedBooks(userId, familyId));
    }

    @GetMapping("/{familyId}/reading-reports")
    public Result<List<FamilyReadingReportVO>> getChildReadingReports(
            @PathVariable Long familyId,
            @RequestParam(defaultValue = "week") String period) {
        Long userId = TenantContext.getTenantId();
        return Result.success(familyService.getChildReadingReports(userId, familyId, period));
    }

    @GetMapping("/{familyId}/storage")
    public Result<QuotaVO> getFamilyStorage(@PathVariable Long familyId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(familyService.getFamilySharedStorage(userId, familyId));
    }
}
