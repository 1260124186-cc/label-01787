package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.annotation.RequirePermission;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.ClassroomStatsVO;
import com.xiaoan.bookstore.entity.Classroom;
import com.xiaoan.bookstore.service.ClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/classrooms")
@RequiredArgsConstructor
public class AdminClassroomController {

    private final ClassroomService classroomService;

    @GetMapping
    @Log("查看班级列表")
    @RequirePermission("classroom:view")
    public Result<IPage<Classroom>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(classroomService.adminClassroomList(page, size, keyword, status));
    }

    @GetMapping("/dashboard")
    @Log("班级统计概览")
    @RequirePermission("classroom:view")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(classroomService.adminDashboard());
    }

    @PutMapping("/{id}/close")
    @Log("关闭班级")
    @RequirePermission("classroom:ban")
    public Result<Void> close(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long adminId = TenantContext.getTenantId();
        String reason = body.get("reason");
        if (reason == null || reason.isEmpty()) {
            return Result.error(400, "关闭原因不能为空");
        }
        classroomService.closeClassroom(adminId, id, reason);
        return Result.success();
    }

    @PutMapping("/{id}/reopen")
    @Log("重新开放班级")
    @RequirePermission("classroom:ban")
    public Result<Void> reopen(@PathVariable Long id) {
        Long adminId = TenantContext.getTenantId();
        classroomService.reopenClassroom(adminId, id);
        return Result.success();
    }

    @GetMapping("/{id}/stats")
    @Log("查看班级统计")
    @RequirePermission("assignment:view")
    public Result<ClassroomStatsVO> stats(@PathVariable Long id) {
        Long adminId = TenantContext.getTenantId();
        return Result.success(classroomService.getClassroomStats(adminId, id));
    }

    @PutMapping("/users/{userId}/teacher")
    @Log("设置教师角色")
    @RequirePermission("classroom:ban")
    public Result<Void> setTeacherRole(@PathVariable Long userId, @RequestBody Map<String, Boolean> body) {
        Boolean isTeacher = body.get("isTeacher");
        if (isTeacher == null) {
            return Result.error(400, "参数无效");
        }
        classroomService.setTeacherRole(userId, isTeacher);
        return Result.success();
    }
}
