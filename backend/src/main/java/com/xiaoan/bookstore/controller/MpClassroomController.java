package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.*;
import com.xiaoan.bookstore.entity.Classroom;
import com.xiaoan.bookstore.entity.ClassroomAssignment;
import com.xiaoan.bookstore.entity.ClassroomMember;
import com.xiaoan.bookstore.entity.ClassroomSubmission;
import com.xiaoan.bookstore.service.ClassroomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mp/classrooms")
@RequiredArgsConstructor
public class MpClassroomController {

    private final ClassroomService classroomService;

    @PostMapping
    @Log("创建班级")
    public Result<Classroom> createClassroom(@Valid @RequestBody ClassroomCreateDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(classroomService.createClassroom(userId, dto));
    }

    @PostMapping("/join")
    @Log("加入班级")
    public Result<ClassroomMember> joinClassroom(@Valid @RequestBody ClassroomJoinDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(classroomService.joinClassroom(userId, dto));
    }

    @PostMapping("/{classroomId}/leave")
    @Log("退出班级")
    public Result<Void> leaveClassroom(@PathVariable Long classroomId) {
        Long userId = TenantContext.getTenantId();
        classroomService.leaveClassroom(userId, classroomId);
        return Result.success();
    }

    @GetMapping("/my")
    public Result<List<Classroom>> myClassrooms() {
        Long userId = TenantContext.getTenantId();
        return Result.success(classroomService.myClassrooms(userId));
    }

    @GetMapping("/{classroomId}")
    public Result<ClassroomVO> getClassroomDetail(@PathVariable Long classroomId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(classroomService.getClassroomDetail(userId, classroomId));
    }

    @PostMapping("/{classroomId}/assignments")
    @Log("布置作业")
    public Result<ClassroomAssignment> createAssignment(@PathVariable Long classroomId,
                                                         @Valid @RequestBody AssignmentCreateDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(classroomService.createAssignment(userId, classroomId, dto));
    }

    @GetMapping("/{classroomId}/assignments")
    public Result<List<AssignmentVO>> listAssignments(@PathVariable Long classroomId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(classroomService.listAssignments(userId, classroomId));
    }

    @GetMapping("/assignments/{assignmentId}")
    public Result<AssignmentVO> getAssignmentDetail(@PathVariable Long assignmentId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(classroomService.getAssignmentDetail(userId, assignmentId));
    }

    @PostMapping("/assignments/{assignmentId}/submit")
    @Log("提交作业")
    public Result<ClassroomSubmission> submitAssignment(@PathVariable Long assignmentId,
                                                         @Valid @RequestBody SubmissionDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(classroomService.submitAssignment(userId, assignmentId, dto));
    }

    @GetMapping("/assignments/{assignmentId}/my-submission")
    public Result<SubmissionVO> getMySubmission(@PathVariable Long assignmentId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(classroomService.getMySubmission(userId, assignmentId));
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public Result<IPage<SubmissionVO>> listSubmissions(@PathVariable Long assignmentId,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(classroomService.listSubmissions(userId, assignmentId, page, size));
    }

    @PostMapping("/submissions/{submissionId}/grade")
    @Log("批改作业")
    public Result<ClassroomSubmission> gradeSubmission(@PathVariable Long submissionId,
                                                        @Valid @RequestBody GradeDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(classroomService.gradeSubmission(userId, submissionId, dto));
    }

    @PostMapping("/assignments/{assignmentId}/remind")
    @Log("催交作业")
    public Result<Void> sendReminder(@PathVariable Long assignmentId,
                                      @Valid @RequestBody ReminderDTO dto) {
        Long userId = TenantContext.getTenantId();
        classroomService.sendReminder(userId, assignmentId, dto);
        return Result.success();
    }

    @PostMapping("/assignments/{assignmentId}/batch-remind")
    @Log("批量催交")
    public Result<Void> batchRemind(@PathVariable Long assignmentId) {
        Long userId = TenantContext.getTenantId();
        classroomService.batchRemind(userId, assignmentId);
        return Result.success();
    }

    @GetMapping("/{classroomId}/stats")
    public Result<ClassroomStatsVO> getClassroomStats(@PathVariable Long classroomId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(classroomService.getClassroomStats(userId, classroomId));
    }
}
