package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.*;
import com.xiaoan.bookstore.entity.BookGroup;
import com.xiaoan.bookstore.entity.GroupDynamic;
import com.xiaoan.bookstore.entity.GroupMember;
import com.xiaoan.bookstore.entity.GroupPlanMember;
import com.xiaoan.bookstore.entity.GroupReadingPlan;
import com.xiaoan.bookstore.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mp/groups")
@RequiredArgsConstructor
public class MpGroupController {

    private final GroupService groupService;

    @PostMapping
    @Log("创建读书小组")
    public Result<BookGroup> create(@Valid @RequestBody GroupCreateDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(groupService.createGroup(userId, dto));
    }

    @PostMapping("/join")
    @Log("加入读书小组")
    public Result<GroupMember> join(@Valid @RequestBody GroupJoinDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(groupService.joinGroup(userId, dto));
    }

    @PostMapping("/{groupId}/leave")
    @Log("退出读书小组")
    public Result<Void> leave(@PathVariable Long groupId) {
        Long userId = TenantContext.getTenantId();
        groupService.leaveGroup(userId, groupId);
        return Result.success();
    }

    @GetMapping("/my")
    public Result<List<BookGroup>> myGroups() {
        Long userId = TenantContext.getTenantId();
        return Result.success(groupService.myGroups(userId));
    }

    @GetMapping("/{groupId}")
    public Result<GroupVO> detail(@PathVariable Long groupId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(groupService.getGroupDetail(userId, groupId));
    }

    @PutMapping("/{groupId}/member-setting")
    @Log("更新小组成员设置")
    public Result<Void> updateMemberSetting(@PathVariable Long groupId,
                                             @Valid @RequestBody GroupMemberUpdateDTO dto) {
        Long userId = TenantContext.getTenantId();
        groupService.updateMemberSetting(userId, groupId, dto);
        return Result.success();
    }

    @GetMapping("/{groupId}/rank")
    public Result<List<GroupRankVO>> weekRank(@PathVariable Long groupId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(groupService.getWeekRank(userId, groupId));
    }

    @PostMapping("/{groupId}/plans")
    @Log("创建共读计划")
    public Result<GroupReadingPlan> createPlan(@PathVariable Long groupId,
                                                @Valid @RequestBody GroupPlanCreateDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(groupService.createPlan(userId, groupId, dto));
    }

    @GetMapping("/{groupId}/plans")
    public Result<List<GroupReadingPlan>> listPlans(@PathVariable Long groupId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(groupService.listPlans(userId, groupId));
    }

    @GetMapping("/plans/{planId}")
    public Result<GroupPlanVO> planDetail(@PathVariable Long planId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(groupService.getPlanDetail(userId, planId));
    }

    @PostMapping("/plans/{planId}/join")
    @Log("加入共读计划")
    public Result<GroupPlanMember> joinPlan(@PathVariable Long planId,
                                             @Valid @RequestBody GroupPlanJoinDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(groupService.joinPlan(userId, planId, dto));
    }

    @PostMapping("/{groupId}/dynamics")
    @Log("发布小组动态")
    public Result<GroupDynamic> createDynamic(@PathVariable Long groupId,
                                               @Valid @RequestBody GroupDynamicCreateDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(groupService.createDynamic(userId, groupId, dto));
    }

    @GetMapping("/{groupId}/dynamics")
    public Result<IPage<GroupDynamicVO>> listDynamics(@PathVariable Long groupId,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(groupService.listDynamics(userId, groupId, page, size));
    }

    @PostMapping("/dynamics/{dynamicId}/like")
    @Log("点赞小组动态")
    public Result<Void> toggleLike(@PathVariable Long dynamicId) {
        Long userId = TenantContext.getTenantId();
        groupService.toggleLike(userId, dynamicId);
        return Result.success();
    }
}
