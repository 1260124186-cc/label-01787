package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.annotation.RateLimit;
import com.xiaoan.bookstore.annotation.RequirePermission;
import com.xiaoan.bookstore.annotation.SensitiveOperation;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.AdminLoginDTO;
import com.xiaoan.bookstore.entity.OperationLog;
import com.xiaoan.bookstore.entity.Permission;
import com.xiaoan.bookstore.entity.Role;
import com.xiaoan.bookstore.service.AdminService;
import com.xiaoan.bookstore.service.ContentComplianceService;
import com.xiaoan.bookstore.service.RbacService;
import com.xiaoan.bookstore.service.ReadingPlanService;
import com.xiaoan.bookstore.service.SensitiveOperationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final RbacService rbacService;
    private final SensitiveOperationService sensitiveOperationService;
    private final ContentComplianceService contentComplianceService;
    private final ReadingPlanService readingPlanService;

    @PostMapping("/login")
    @RateLimit(type = RateLimit.RateLimitType.IP, limit = 10, windowSeconds = 60)
    public Result<Map<String, Object>> login(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.success(adminService.login(dto));
    }

    @GetMapping("/dashboard")
    @Log("查看仪表盘")
    @RequirePermission("dashboard:view")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(adminService.dashboard());
    }

    @GetMapping("/users")
    @Log("查看用户列表")
    @RequirePermission("user:view")
    public Result<?> userList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.userList(page, size, keyword));
    }

    @PutMapping("/users/{id}/disable")
    @Log("禁用用户")
    @RequirePermission("user:disable")
    @SensitiveOperation("disable_user")
    public Result<Void> disableUser(@PathVariable Long id) {
        adminService.disableUser(id);
        return Result.success();
    }

    @GetMapping("/books")
    @Log("查看书籍列表")
    @RequirePermission("book:view")
    public Result<?> bookList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String format) {
        return Result.success(adminService.bookList(page, size, keyword, format));
    }

    @GetMapping("/books/format-stats")
    @Log("查看格式统计")
    @RequirePermission("book:view")
    public Result<Map<String, Object>> bookFormatStats() {
        return Result.success(adminService.bookFormatStats());
    }

    @DeleteMapping("/books/{id}")
    @Log("删除书籍")
    @RequirePermission("book:delete")
    @SensitiveOperation("delete_book")
    public Result<Void> deleteBook(@PathVariable Long id) {
        adminService.deleteBook(id);
        return Result.success();
    }

    @DeleteMapping("/books/batch")
    @Log("批量删除书籍")
    @RequirePermission("book:delete")
    @SensitiveOperation("batch_delete_books")
    public Result<Map<String, Object>> batchDeleteBooks(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        return Result.success(adminService.batchDeleteBooks(ids));
    }

    @PutMapping("/books/batch/take-down")
    @Log("批量下架书籍")
    @RequirePermission("book:update")
    @SensitiveOperation("batch_take_down_books")
    public Result<Map<String, Object>> batchTakeDownBooks(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        return Result.success(adminService.batchTakeDownBooks(ids));
    }

    @GetMapping("/books/{id}/uploader")
    @RequirePermission("book:view")
    public Result<Map<String, Object>> getBookUploader(@PathVariable Long id) {
        return Result.success(adminService.getBookUploader(id));
    }

    @GetMapping("/books/{id}/preview")
    @RequirePermission("book:view")
    public Result<Map<String, Object>> previewBookPdf(
            @PathVariable Long id,
            @RequestParam(defaultValue = "3") int pages) {
        return Result.success(adminService.previewBookPdf(id, pages));
    }

    @GetMapping("/logs")
    @Log("查看日志")
    @RequirePermission("log:view")
    public Result<?> logList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startTime,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endTime,
            @RequestParam(required = false) Integer userType) {
        return Result.success(adminService.logList(page, size, action, ip, startTime, endTime, userType));
    }

    @GetMapping("/logs/export")
    @Log("导出操作日志CSV")
    @RequirePermission("log:export")
    public void exportLogsCsv(
            HttpServletResponse response,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startTime,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endTime,
            @RequestParam(required = false) Integer userType) throws java.io.IOException {
        adminService.exportLogsCsv(response, action, ip, startTime, endTime, userType);
    }

    @GetMapping("/logs/{id}")
    @RequirePermission("log:view")
    public Result<OperationLog> logDetail(@PathVariable Long id) {
        return Result.success(adminService.logDetail(id));
    }

    @GetMapping("/download-logs")
    @Log("查看文件下载日志")
    @RequirePermission("download_log:view")
    public Result<?> downloadLogList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer userType) {
        return Result.success(adminService.downloadLogList(page, size, userId, userType));
    }

    @GetMapping("/admins")
    @Log("查看管理员列表")
    @RequirePermission("admin:view")
    public Result<?> adminList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.adminList(page, size, keyword));
    }

    @PostMapping("/admins")
    @Log("创建管理员")
    @RequirePermission("admin:create")
    @SensitiveOperation("create_admin")
    public Result<Map<String, Object>> createAdmin(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String nickname = (String) body.get("nickname");
        Long roleId = body.get("roleId") != null ? ((Number) body.get("roleId")).longValue() : null;
        String initPassword = (String) body.get("initPassword");
        return Result.success(adminService.createAdmin(username, nickname, roleId, initPassword));
    }

    @PutMapping("/admins/{id}/nickname")
    @Log("修改管理员昵称")
    @RequirePermission("admin:update")
    public Result<Void> updateAdminNickname(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminService.updateAdminNickname(id, body.get("nickname"));
        return Result.success();
    }

    @PutMapping("/admins/{id}/role")
    @Log("修改管理员角色")
    @RequirePermission("admin:update")
    @SensitiveOperation("update_admin_role")
    public Result<Void> updateAdminRole(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long roleId = body.get("roleId") != null ? ((Number) body.get("roleId")).longValue() : null;
        adminService.updateAdminRole(id, roleId);
        return Result.success();
    }

    @PutMapping("/admins/{id}/password")
    @Log("重置管理员密码")
    @RequirePermission("admin:update")
    @SensitiveOperation("reset_admin_password")
    public Result<Map<String, String>> resetAdminPassword(@PathVariable Long id) {
        return Result.success(adminService.resetAdminPassword(id));
    }

    @PutMapping("/admins/{id}/status")
    @Log("启用/禁用管理员")
    @RequirePermission("admin:update")
    public Result<Void> toggleAdminStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        adminService.toggleAdminStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/admins/{id}")
    @Log("删除管理员")
    @RequirePermission("admin:delete")
    @SensitiveOperation("delete_admin")
    public Result<Void> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return Result.success();
    }

    @GetMapping("/roles")
    @Log("查看角色列表")
    @RequirePermission("role:view")
    public Result<List<Role>> roleList() {
        return Result.success(rbacService.listRoles());
    }

    @GetMapping("/roles/{id}")
    @RequirePermission("role:view")
    public Result<Role> roleDetail(@PathVariable Long id) {
        return Result.success(rbacService.getRoleWithPermissions(id));
    }

    @PutMapping("/roles/{id}/permissions")
    @Log("修改角色权限")
    @RequirePermission("role:update")
    @SensitiveOperation("update_role_permissions")
    public Result<Void> updateRolePermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        rbacService.updateRolePermissions(id, body.get("permissionIds"));
        return Result.success();
    }

    @GetMapping("/permissions")
    @RequirePermission("role:view")
    public Result<List<Permission>> permissionList() {
        return Result.success(rbacService.listPermissions());
    }

    @GetMapping("/permissions/mine")
    public Result<List<String>> myPermissions() {
        Long roleId = TenantContext.getRoleId();
        return Result.success(rbacService.getPermissionCodesByRoleId(roleId));
    }

    @PostMapping("/sensitive/confirm-token")
    public Result<Map<String, String>> requestConfirmToken(@RequestBody Map<String, String> body) {
        Long adminId = TenantContext.getTenantId();
        String operation = body.get("operation");
        if (operation == null || operation.isEmpty()) {
            return Result.error(400, "操作标识不能为空");
        }
        String token = sensitiveOperationService.generateConfirmToken(adminId, operation);
        Map<String, String> result = Map.of("confirmToken", token);
        return Result.success(result);
    }

    @GetMapping("/complaints")
    @Log("查看版权申诉列表")
    @RequirePermission("complaint:view")
    public Result<?> complaintList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.success(contentComplianceService.complaintList(page, size, status));
    }

    @PutMapping("/complaints/{id}/handle")
    @Log("处理版权申诉")
    @RequirePermission("complaint:handle")
    @SensitiveOperation("handle_complaint")
    public Result<Void> handleComplaint(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object statusObj = body.get("status");
        Integer status = null;
        if (statusObj instanceof Number) {
            status = ((Number) statusObj).intValue();
        }
        String handleResult = (String) body.get("handleResult");
        Long bookId = null;
        Object bookIdObj = body.get("bookId");
        if (bookIdObj instanceof Number) {
            bookId = ((Number) bookIdObj).longValue();
        }
        if (status == null) {
            return Result.error(400, "处理状态不能为空");
        }
        Long handlerId = TenantContext.getTenantId();
        contentComplianceService.handleComplaint(id, status, handleResult, handlerId, bookId);
        return Result.success();
    }

    @GetMapping("/audits")
    @Log("查看内容审核列表")
    @RequirePermission("audit:view")
    public Result<?> auditList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer result,
            @RequestParam(required = false) Integer targetType) {
        return Result.success(contentComplianceService.auditList(page, size, result, targetType));
    }

    @GetMapping("/compliance/report")
    @Log("生成合规审计报告")
    @RequirePermission("audit:report")
    public Result<Map<String, Object>> complianceReport(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startTime,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endTime) {
        if (startTime == null) {
            startTime = java.time.LocalDateTime.now().minusDays(30);
        }
        if (endTime == null) {
            endTime = java.time.LocalDateTime.now();
        }
        return Result.success(contentComplianceService.generateComplianceReport(startTime, endTime));
    }

    @GetMapping("/reading-stats")
    @Log("查看阅读统计")
    @RequirePermission("dashboard:view")
    public Result<Map<String, Object>> readingStats(
            @RequestParam(defaultValue = "7") Integer days) {
        return Result.success(adminService.readingStats(days));
    }

    @GetMapping("/reading-behavior")
    @Log("查看阅读行为分析")
    @RequirePermission("dashboard:view")
    public Result<Map<String, Object>> readingBehaviorAnalysis(
            @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(adminService.readingBehaviorAnalysis(days));
    }

    @GetMapping("/reading-plans/stats")
    @Log("查看阅读计划统计")
    @RequirePermission("reading_plan:view")
    public Result<Map<String, Object>> readingPlanStats() {
        return Result.success(readingPlanService.adminStats());
    }
}
