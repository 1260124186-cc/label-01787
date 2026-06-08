package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.NotificationVO;
import com.xiaoan.bookstore.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
public class MpNotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public Result<IPage<NotificationVO>> getMyNotifications(
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(notificationService.getMyNotifications(userId, type, page, size));
    }

    @GetMapping("/notifications/unread-count")
    public Result<Map<Integer, Long>> getUnreadCount() {
        Long userId = TenantContext.getTenantId();
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @GetMapping("/notifications/{id}")
    public Result<NotificationVO> getNotificationDetail(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        return Result.success(notificationService.getDetail(id, userId));
    }

    @PutMapping("/notifications/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    @PutMapping("/notifications/read-all")
    public Result<Void> markAllAsRead(@RequestParam(required = false) Integer type) {
        Long userId = TenantContext.getTenantId();
        notificationService.markAllAsRead(userId, type);
        return Result.success();
    }
}
