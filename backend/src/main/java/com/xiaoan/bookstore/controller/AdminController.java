package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.dto.AdminLoginDTO;
import com.xiaoan.bookstore.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.success(adminService.login(dto));
    }

    @GetMapping("/dashboard")
    @Log("查看仪表盘")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(adminService.dashboard());
    }

    @GetMapping("/users")
    public Result<?> userList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.userList(page, size, keyword));
    }

    @GetMapping("/books")
    public Result<?> bookList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.bookList(page, size, keyword));
    }

    @GetMapping("/logs")
    public Result<?> logList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(adminService.logList(page, size));
    }

    @GetMapping("/admins")
    public Result<?> adminList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.adminList(page, size, keyword));
    }

    @PutMapping("/admins/{id}/nickname")
    @Log("修改管理员昵称")
    public Result<Void> updateAdminNickname(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminService.updateAdminNickname(id, body.get("nickname"));
        return Result.success();
    }
}
