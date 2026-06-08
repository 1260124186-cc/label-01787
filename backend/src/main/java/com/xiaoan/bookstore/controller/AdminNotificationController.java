package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.annotation.RequirePermission;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.dto.NotificationTemplateDTO;
import com.xiaoan.bookstore.dto.SendAnnouncementDTO;
import com.xiaoan.bookstore.entity.Notification;
import com.xiaoan.bookstore.entity.NotificationTemplate;
import com.xiaoan.bookstore.service.NotificationService;
import com.xiaoan.bookstore.service.NotificationTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final NotificationTemplateService templateService;

    @GetMapping("/notifications")
    @RequirePermission("notification:view")
    public Result<IPage<Notification>> getNotifications(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(notificationService.getAdminNotifications(type, isRead, page, size));
    }

    @PostMapping("/notifications/send")
    @RequirePermission("notification:send")
    @Log("发送公告")
    public Result<Void> sendAnnouncement(@Valid @RequestBody SendAnnouncementDTO dto, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("admin_id");
        notificationService.sendAnnouncement(dto, adminId);
        return Result.success();
    }

    @GetMapping("/notification-templates")
    @RequirePermission("template:view")
    public Result<IPage<NotificationTemplate>> getTemplates(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(templateService.getTemplates(type, status, page, size));
    }

    @GetMapping("/notification-templates/enabled")
    @RequirePermission("template:view")
    public Result<List<NotificationTemplate>> getEnabledTemplates() {
        return Result.success(templateService.getAllEnabledTemplates());
    }

    @GetMapping("/notification-templates/{id}")
    @RequirePermission("template:view")
    public Result<NotificationTemplate> getTemplate(@PathVariable Long id) {
        return Result.success(templateService.getById(id));
    }

    @PostMapping("/notification-templates")
    @RequirePermission("template:create")
    @Log("创建消息模板")
    public Result<NotificationTemplate> createTemplate(@Valid @RequestBody NotificationTemplateDTO dto) {
        return Result.success(templateService.create(dto));
    }

    @PutMapping("/notification-templates/{id}")
    @RequirePermission("template:update")
    @Log("更新消息模板")
    public Result<NotificationTemplate> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody NotificationTemplateDTO dto) {
        return Result.success(templateService.update(id, dto));
    }

    @DeleteMapping("/notification-templates/{id}")
    @RequirePermission("template:delete")
    @Log("删除消息模板")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        templateService.delete(id);
        return Result.success();
    }

    @PutMapping("/notification-templates/{id}/toggle-status")
    @RequirePermission("template:update")
    @Log("切换模板状态")
    public Result<Void> toggleTemplateStatus(@PathVariable Long id) {
        templateService.toggleStatus(id);
        return Result.success();
    }
}
