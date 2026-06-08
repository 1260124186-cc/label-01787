package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.annotation.RequirePermission;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.dto.BackupTaskVO;
import com.xiaoan.bookstore.dto.StorageStatsVO;
import com.xiaoan.bookstore.entity.BackupTask;
import com.xiaoan.bookstore.entity.User;
import com.xiaoan.bookstore.mapper.UserMapper;
import com.xiaoan.bookstore.mapper.BackupTaskMapper;
import com.xiaoan.bookstore.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBackupController {

    private final BackupService backupService;
    private final UserMapper userMapper;
    private final BackupTaskMapper backupTaskMapper;

    @GetMapping("/backups")
    @Log("查看备份任务列表")
    @RequirePermission("backup:view")
    public Result<Page<BackupTaskVO>> backupList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Page<BackupTask> taskPage = backupService.getAllTasks(status, page, size);
        Page<BackupTaskVO> voPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        List<BackupTaskVO> voList = new ArrayList<>();
        for (BackupTask task : taskPage.getRecords()) {
            voList.add(convertToVO(task));
        }
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    @GetMapping("/backups/{id}")
    @Log("查看备份任务详情")
    @RequirePermission("backup:view")
    public Result<BackupTaskVO> backupDetail(@PathVariable Long id) {
        BackupTask task = backupTaskMapper.selectById(id);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(convertToVO(task));
    }

    @GetMapping("/backups/{id}/download")
    @Log("管理员下载备份文件")
    @RequirePermission("backup:export")
    public ResponseEntity<PathResource> downloadBackup(@PathVariable Long id) throws IOException {
        BackupTask task = backupTaskMapper.selectById(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        Path filePath = backupService.getBackupFilePath(task);

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        String encodedFileName = URLEncoder.encode(task.getFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .contentLength(Files.size(filePath))
                .body(new PathResource(filePath));
    }

    @DeleteMapping("/backups/{id}")
    @Log("管理员删除备份任务")
    @RequirePermission("backup:delete")
    public Result<Void> deleteBackup(@PathVariable Long id) {
        backupService.deleteTask(id);
        return Result.success();
    }

    @GetMapping("/storage")
    @Log("查看存储用量统计")
    @RequirePermission("storage:view")
    public Result<StorageStatsVO> getStorageStats() {
        return Result.success(backupService.getStorageStats());
    }

    private BackupTaskVO convertToVO(BackupTask task) {
        BackupTaskVO vo = new BackupTaskVO();
        vo.setId(task.getId());
        vo.setUserId(task.getUserId());
        if (task.getUserId() != null) {
            User user = userMapper.selectById(task.getUserId());
            vo.setUserNickname(user != null ? user.getNickname() : "未知用户");
        } else {
            vo.setUserNickname("未知用户");
        }
        vo.setTaskType(task.getTaskType());
        vo.setTaskTypeText(task.getTaskType() != null && task.getTaskType() == Constants.BACKUP_TYPE_EXPORT ? "导出" : "导入");
        vo.setStatus(task.getStatus());
        vo.setStatusText(getStatusText(task.getStatus()));
        vo.setFileName(task.getFileName());
        vo.setFileSize(task.getFileSize());
        vo.setFileSizeText(formatFileSize(task.getFileSize()));
        vo.setBookCount(task.getBookCount() != null ? task.getBookCount() : 0);
        vo.setAnnotationCount(task.getAnnotationCount() != null ? task.getAnnotationCount() : 0);
        vo.setRecordCount(task.getRecordCount() != null ? task.getRecordCount() : 0);
        vo.setCategoryCount(task.getCategoryCount() != null ? task.getCategoryCount() : 0);
        vo.setProgress(task.getProgress() != null ? task.getProgress() : 0);
        vo.setErrorMessage(task.getErrorMessage());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (task.getCreatedAt() != null) {
            vo.setCreatedAt(task.getCreatedAt().format(formatter));
        }
        if (task.getUpdatedAt() != null) {
            vo.setUpdatedAt(task.getUpdatedAt().format(formatter));
        }
        if (task.getExpiredAt() != null) {
            vo.setExpiredAt(task.getExpiredAt().format(formatter));
        }
        return vo;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已完成";
            case 3 -> "失败";
            default -> "未知";
        };
    }

    private String formatFileSize(Long size) {
        if (size == null || size == 0) return "0 B";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
