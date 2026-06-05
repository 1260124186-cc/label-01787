package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.annotation.RequirePermission;
import com.xiaoan.bookstore.annotation.SensitiveOperation;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.AdminLoginDTO;
import com.xiaoan.bookstore.entity.Permission;
import com.xiaoan.bookstore.entity.Role;
import com.xiaoan.bookstore.service.AdminService;
import com.xiaoan.bookstore.service.ContentComplianceService;
import com.xiaoan.bookstore.service.RbacService;
import com.xiaoan.bookstore.service.SensitiveOperationService;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/login")
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
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.bookList(page, size, keyword));
    }

    @DeleteMapping("/books/{id}")
    @Log("删除书籍")
    @RequirePermission("book:delete")
    @SensitiveOperation("delete_book")
    public Result<Void> deleteBook(@PathVariable Long id) {
        adminService.deleteBook(id);
        return Result.success();
    }

    @GetMapping("/logs")
    @Log("查看日志")
    @RequirePermission("log:view")
    public Result<?> logList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(adminService.logList(page, size));
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

    @PutMapping("/admins/{id}/nickname")
    @Log("修改管理员昵称")
    @RequirePermission("admin:update")
    public Result<Void> updateAdminNickname(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminService.updateAdminNickname(id, body.get("nickname"));
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
        if (status == null) {
            return Result.error(400, "处理状态不能为空");
        }
        Long handlerId = TenantContext.getTenantId();
        contentComplianceService.handleComplaint(id, status, handleResult, handlerId);
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
}
