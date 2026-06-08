package com.xiaoan.bookstore.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.ExportDataDTO;
import com.xiaoan.bookstore.entity.BackupTask;
import com.xiaoan.bookstore.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/mp/backup")
@RequiredArgsConstructor
public class MpBackupController {

    private final BackupService backupService;

    @PostMapping("/export")
    @Log("创建导出任务")
    public Result<BackupTask> createExportTask() {
        Long userId = TenantContext.getTenantId();
        BackupTask task = backupService.createExportTask(userId);
        backupService.processExportAsync(task.getId());
        return Result.success(task);
    }

    @PostMapping("/import")
    @Log("创建导入任务")
    public Result<BackupTask> createImportTask(@RequestParam("file") MultipartFile file) {
        Long userId = TenantContext.getTenantId();
        BackupTask task = backupService.createImportTask(userId, file);
        backupService.processImportAsync(task.getId());
        return Result.success(task);
    }

    @GetMapping("/tasks")
    public Result<Page<BackupTask>> getTasks(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(backupService.getUserTasks(userId, page, size));
    }

    @GetMapping("/tasks/{id}")
    public Result<BackupTask> getTaskDetail(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        return Result.success(backupService.getTaskDetail(userId, id));
    }

    @GetMapping("/tasks/{id}/download")
    @Log("下载备份文件")
    public ResponseEntity<PathResource> downloadBackup(@PathVariable Long id) throws IOException {
        Long userId = TenantContext.getTenantId();
        BackupTask task = backupService.getTaskDetail(userId, id);
        if (task.getFilePath() == null || task.getFilePath().isBlank()) {
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

    @GetMapping("/tasks/{id}/import-result")
    public Result<ExportDataDTO.ImportResult> getImportResult(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        return Result.success(backupService.getImportResult(userId, id));
    }

    @DeleteMapping("/tasks/{id}")
    @Log("删除备份任务")
    public Result<Void> deleteTask(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        BackupTask task = backupService.getTaskDetail(userId, id);
        backupService.deleteTask(task.getId());
        return Result.success();
    }
}
