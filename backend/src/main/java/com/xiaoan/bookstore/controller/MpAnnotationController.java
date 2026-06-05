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
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(annotationService.list(userId, bookId, type, page, size));
    }

    @PutMapping("/{id}")
    @Log("更新批注")
    public Result<Void> update(@PathVariable Long id,
                                @Valid @RequestBody AnnotationDTO dto) {
        Long userId = TenantContext.getTenantId();
        annotationService.update(userId, id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Log("删除批注")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        annotationService.delete(userId, id);
        return Result.success();
    }
}
