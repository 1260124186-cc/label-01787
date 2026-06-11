package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.AnnotationDTO;
import com.xiaoan.bookstore.entity.Annotation;
import com.xiaoan.bookstore.service.AnnotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mp/annotations")
@RequiredArgsConstructor
public class MpAnnotationController {

    private final AnnotationService annotationService;

    @PostMapping
    @Log("添加批注")
    public Result<Annotation> create(@Valid @RequestBody AnnotationDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(annotationService.create(userId, dto));
    }

    @GetMapping
    public Result<?> list(@RequestParam(required = false) Long bookId,
                          @RequestParam(required = false) Integer type,
                          @RequestParam(required = false) String tag,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(annotationService.list(userId, bookId, type, tag, page, size));
    }

    @GetMapping("/tags")
    public Result<List<String>> listTags() {
        Long userId = TenantContext.getTenantId();
        return Result.success(annotationService.listTags(userId));
    }

    @PutMapping("/{id}")
    @Log("更新批注")
    public Result<Void> update(@PathVariable Long id,
                                @Valid @RequestBody AnnotationDTO dto) {
        Long userId = TenantContext.getTenantId();
        annotationService.update(userId, id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/pin")
    @Log("切换批注置顶")
    public Result<Void> togglePin(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        annotationService.togglePin(userId, id);
        return Result.success();
    }

    @PutMapping("/{id}/color")
    @Log("更新批注颜色")
    public Result<Void> updateColor(@PathVariable Long id,
                                     @RequestParam String color) {
        Long userId = TenantContext.getTenantId();
        annotationService.updateColor(userId, id, color);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Log("删除批注")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        annotationService.delete(userId, id);
        return Result.success();
    }

    @GetMapping("/export/markdown")
    @Log("导出批注Markdown")
    public Result<String> exportMarkdown(@RequestParam Long bookId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(annotationService.exportMarkdown(userId, bookId));
    }

    @GetMapping("/book/{bookId}/all")
    public Result<List<Annotation>> listAllByBook(@PathVariable Long bookId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(annotationService.listAllByBook(userId, bookId));
    }
}
