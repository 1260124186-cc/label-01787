package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.OrderCreateDTO;
import com.xiaoan.bookstore.dto.PointsExchangeDTO;
import com.xiaoan.bookstore.entity.UserMembership;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.service.MembershipService;
import com.xiaoan.bookstore.service.PointsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
public class MpMembershipController {

    private final MembershipService membershipService;
    private final PointsService pointsService;

    @GetMapping("/membership/plans")
    public Result<?> plans() {
        return Result.success(membershipService.listPlans());
    }

    @GetMapping("/membership/quota")
    public Result<?> quota() {
        Long userId = TenantContext.getTenantId();
        return Result.success(membershipService.getQuota(userId));
    }

    @GetMapping("/membership/status")
    public Result<UserMembership> status() {
        Long userId = TenantContext.getTenantId();
        UserMembership um = membershipService.getOrCreateUserMembership(userId);
        return Result.success(um);
    }

    @PostMapping("/membership/order")
    public Result<?> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(membershipService.createOrder(userId, dto));
    }

    @PostMapping("/membership/pay-callback")
    public Result<Void> payCallback(@RequestBody Map<String, String> body) {
        String orderNo = body.get("orderNo");
        String transactionId = body.get("transactionId");
        membershipService.handlePayCallback(orderNo, transactionId);
        return Result.success();
    }

    @GetMapping("/points/info")
    public Result<?> pointsInfo() {
        Long userId = TenantContext.getTenantId();
        return Result.success(pointsService.getPointsInfo(userId));
    }

    @PostMapping("/points/checkin")
    @Log("每日打卡")
    public Result<String> checkin() {
        Long userId = TenantContext.getTenantId();
        boolean result = pointsService.dailyCheckIn(userId);
        return Result.success(result ? "签到成功" : "今日已签到");
    }

    @GetMapping("/points/history")
    public Result<?> pointsHistory(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(pointsService.pointsHistory(userId, page, size));
    }

    @GetMapping("/points/rules")
    public Result<?> pointsRules() {
        return Result.success(pointsService.listRules());
    }

    @PostMapping("/points/exchange")
    @Log("积分兑换")
    public Result<Void> exchange(@Valid @RequestBody PointsExchangeDTO dto) {
        Long userId = TenantContext.getTenantId();
        if (dto.getExchangeType() == Constants.EXCHANGE_TYPE_VIP_DAYS) {
            pointsService.exchangeVipDays(userId, dto.getValue());
        } else if (dto.getExchangeType() == Constants.EXCHANGE_TYPE_STORAGE) {
            pointsService.exchangeStorage(userId, dto.getValue());
        } else {
            throw new BusinessException("无效的兑换类型");
        }
        return Result.success();
    }

    @PostMapping("/membership/ai-usage")
    @Log("AI使用计数")
    public Result<Void> incrementAiUsage() {
        Long userId = TenantContext.getTenantId();
        membershipService.checkAiQuota(userId);
        membershipService.incrementAiUsage(userId);
        return Result.success();
    }
}
