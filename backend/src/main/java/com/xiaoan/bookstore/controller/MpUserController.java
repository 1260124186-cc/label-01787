package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.MpLoginDTO;
import com.xiaoan.bookstore.entity.User;
import com.xiaoan.bookstore.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
public class MpUserController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody MpLoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @GetMapping("/user/profile")
    public Result<User> profile() {
        Long userId = TenantContext.getTenantId();
        return Result.success(userService.getProfile(userId));
    }

    @PutMapping("/user/profile")
    @Log("修改个人信息")
    public Result<Void> updateProfile(@RequestBody Map<String, String> body) {
        Long userId = TenantContext.getTenantId();
        userService.updateProfile(userId, body.get("nickname"), body.get("avatar"));
        return Result.success();
    }

    @PostMapping("/user/avatar")
    @Log("上传头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = TenantContext.getTenantId();
        String avatarUrl = userService.uploadAvatar(userId, file);
        return Result.success(avatarUrl);
    }
}
