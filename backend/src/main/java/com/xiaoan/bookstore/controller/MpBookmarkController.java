package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.BookmarkDTO;
import com.xiaoan.bookstore.entity.Bookmark;
import com.xiaoan.bookstore.service.BookmarkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mp/bookmarks")
@RequiredArgsConstructor
public class MpBookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping
    @Log("添加书签")
    public Result<Bookmark> create(@Valid @RequestBody BookmarkDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookmarkService.create(userId, dto));
    }

    @GetMapping("/book/{bookId}")
    public Result<List<Bookmark>> listByBook(@PathVariable Long bookId,
                                              @RequestParam(required = false) Integer isChapter) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookmarkService.listByBook(userId, bookId, isChapter));
    }

    @GetMapping("/grouped")
    public Result<Map<String, Object>> listGrouped() {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookmarkService.listGroupByBook(userId));
    }

    @GetMapping("/check")
    public Result<Bookmark> checkExists(@RequestParam Long bookId,
                                        @RequestParam Integer pageNum) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookmarkService.getByBookAndPage(userId, bookId, pageNum));
    }

    @PutMapping("/{id}")
    @Log("更新书签")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody BookmarkDTO dto) {
        Long userId = TenantContext.getTenantId();
        bookmarkService.update(userId, id, dto);
        return Result.success();
    }

    @PutMapping("/reorder")
    @Log("书签重排序")
    public Result<Void> reorder(@RequestBody List<Long> ids) {
        Long userId = TenantContext.getTenantId();
        bookmarkService.reorder(userId, ids);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Log("删除书签")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        bookmarkService.delete(userId, id);
        return Result.success();
    }
}
