package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.annotation.RequirePermission;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.dto.MembershipPlanDTO;
import com.xiaoan.bookstore.service.MembershipService;
import com.xiaoan.bookstore.service.PointsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMembershipController {

    private final MembershipService membershipService;
    private final PointsService pointsService;

    @GetMapping("/membership/plans")
    @Log("查看套餐列表")
    @RequirePermission("plan:view")
    public Result<?> listPlans() {
        return Result.success(membershipService.listPlans());
    }

    @PostMapping("/membership/plans")
    @Log("创建套餐")
    @RequirePermission("plan:create")
    public Result<?> createPlan(@Valid @RequestBody MembershipPlanDTO dto) {
        return Result.success(membershipService.createPlan(dto));
    }

    @PutMapping("/membership/plans/{id}")
    @Log("修改套餐")
    @RequirePermission("plan:update")
    public Result<Void> updatePlan(@PathVariable Long id, @Valid @RequestBody MembershipPlanDTO dto) {
        membershipService.updatePlan(id, dto);
        return Result.success();
    }

    @GetMapping("/membership/orders")
    @Log("查看订单列表")
    @RequirePermission("order:view")
    public Result<?> orderList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo) {
        return Result.success(membershipService.adminOrderList(page, size, status, orderNo));
    }

    @GetMapping("/membership/members")
    @Log("查看会员状态")
    @RequirePermission("member:view")
    public Result<?> memberList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(membershipService.adminMemberList(page, size, keyword));
    }

    @GetMapping("/membership/points-rules")
    @Log("查看积分规则")
    @RequirePermission("points_rule:view")
    public Result<?> listPointsRules() {
        return Result.success(pointsService.listRules());
    }

    @PutMapping("/membership/points-rules/{id}")
    @Log("修改积分规则")
    @RequirePermission("points_rule:update")
    public Result<Void> updatePointsRule(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer points = body.get("points");
        Integer dailyLimit = body.get("dailyLimit");
        pointsService.updateRule(id, points, dailyLimit);
        return Result.success();
    }

    @PostMapping("/membership/points-adjust")
    @Log("调整用户积分")
    @RequirePermission("points_adjust")
    public Result<Void> adjustPoints(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        int points = ((Number) body.get("points")).intValue();
        String description = (String) body.get("description");
        pointsService.adminAdjustPoints(userId, points, description);
        return Result.success();
    }
}
