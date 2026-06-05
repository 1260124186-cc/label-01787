package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.CategoryDTO;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.entity.Category;
import com.xiaoan.bookstore.service.BookService;
import com.xiaoan.bookstore.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
public class MpBookController {

    private final BookService bookService;
    private final CategoryService categoryService;

    @PostMapping("/books/upload")
    @Log("上传书籍")
    public Result<Book> upload(@RequestParam("file") MultipartFile file,
                                @RequestParam(required = false) String title,
                                @RequestParam(required = false) String author,
                                @RequestParam(required = false) Long categoryId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.upload(userId, file, title, author, categoryId));
    }

    @GetMapping("/books")
    public Result<?> myBooks(@RequestParam(required = false) Long categoryId,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.myBooks(userId, categoryId, page, size));
    }

    @GetMapping("/books/{id}")
    public Result<Book> detail(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.detail(userId, id));
    }

    @DeleteMapping("/books/{id}")
    @Log("删除书籍")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        bookService.delete(userId, id);
        return Result.success();
    }

    @GetMapping("/books/{id}/page/{pageNum}")
    public ResponseEntity<byte[]> getPageImage(@PathVariable Long id,
                                                @PathVariable int pageNum) {
        Long userId = TenantContext.getTenantId();
        byte[] image = bookService.getPageImage(userId, id, pageNum);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @GetMapping("/books/{id}/toc")
    public Result<List<Map<String, Object>>> getToc(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.getToc(userId, id));
    }

    @GetMapping("/books/{id}/text/{pageNum}")
    public Result<String> getPageText(@PathVariable Long id,
                                       @PathVariable int pageNum) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.getPageText(userId, id, pageNum));
    }

    @PutMapping("/books/{id}/progress")
    public Result<Void> updateProgress(@PathVariable Long id,
                                        @RequestBody Map<String, Integer> body) {
        Long userId = TenantContext.getTenantId();
        Integer lastPage = body.get("lastPage");
        if (lastPage == null || lastPage < 0) {
            throw new com.xiaoan.bookstore.exception.BusinessException("页码参数无效");
        }
        bookService.updateLastPage(userId, id, lastPage);
        return Result.success();
    }

    @PostMapping("/categories")
    @Log("创建分类")
    public Result<Category> createCategory(@Valid @RequestBody CategoryDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(categoryService.create(userId, dto));
    }

    @GetMapping("/categories")
    public Result<List<Category>> categoryList() {
        Long userId = TenantContext.getTenantId();
        return Result.success(categoryService.list(userId));
    }

    @PutMapping("/categories/{id}")
    @Log("更新分类")
    public Result<Void> updateCategory(@PathVariable Long id,
                                        @Valid @RequestBody CategoryDTO dto) {
        Long userId = TenantContext.getTenantId();
        categoryService.update(userId, id, dto);
        return Result.success();
    }

    @DeleteMapping("/categories/{id}")
    @Log("删除分类")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        categoryService.delete(userId, id);
        return Result.success();
    }
}
