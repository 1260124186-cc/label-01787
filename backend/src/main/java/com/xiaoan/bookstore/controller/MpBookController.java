package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.annotation.RateLimit;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.BatchBookDTO;
import com.xiaoan.bookstore.dto.BookDetailVO;
import com.xiaoan.bookstore.dto.BookUpdateDTO;
import com.xiaoan.bookstore.dto.CategoryDTO;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.entity.Category;
import com.xiaoan.bookstore.service.BookService;
import com.xiaoan.bookstore.service.CategoryService;
import com.xiaoan.bookstore.service.PdfPreRenderService;
import com.xiaoan.bookstore.service.SysConfigService;
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
    private final PdfPreRenderService pdfPreRenderService;
    private final SysConfigService sysConfigService;
    private final com.xiaoan.bookstore.service.BookMetadataAggregationService metadataAggregationService;

    @PostMapping("/books/upload")
    @Log("上传书籍")
    @RateLimit(type = RateLimit.RateLimitType.USER, limit = 10, windowSeconds = 3600)
    public Result<Book> upload(@RequestParam("file") MultipartFile file,
                                @RequestParam(required = false) String title,
                                @RequestParam(required = false) String author,
                                @RequestParam(required = false) Long categoryId,
                                @RequestParam(required = false, defaultValue = "0") Integer copyrightDeclared) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.upload(userId, file, title, author, categoryId, copyrightDeclared));
    }

    @GetMapping("/books")
    public Result<?> myBooks(@RequestParam(required = false) Long categoryId,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int size,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false, defaultValue = "upload_time") String sortBy) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.myBooks(userId, categoryId, page, size, keyword, sortBy));
    }

    @GetMapping("/books/{id}")
    public Result<BookDetailVO> detail(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.detail(userId, id));
    }

    @PutMapping("/books/{id}")
    @Log("更新书籍信息")
    public Result<Book> updateBook(@PathVariable Long id, @Valid @RequestBody BookUpdateDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.updateBook(userId, id, dto.getTitle(), dto.getAuthor(), dto.getCategoryId()));
    }

    @DeleteMapping("/books/{id}")
    @Log("删除书籍（移入回收站）")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        bookService.softDelete(userId, id);
        return Result.success();
    }

    @PostMapping("/books/{id}/restore")
    @Log("从回收站恢复书籍")
    public Result<Void> restore(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        bookService.restore(userId, id);
        return Result.success();
    }

    @GetMapping("/trash/books")
    public Result<?> trashList(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.trashList(userId, page, size));
    }

    @PostMapping("/books/batch-delete")
    @Log("批量删除书籍（移入回收站）")
    public Result<Void> batchDelete(@RequestBody BatchBookDTO dto) {
        Long userId = TenantContext.getTenantId();
        bookService.batchDelete(userId, dto.getBookIds());
        return Result.success();
    }

    @PostMapping("/books/batch-move-category")
    @Log("批量移动书籍分类")
    public Result<Void> batchMoveCategory(@RequestBody BatchBookDTO dto) {
        Long userId = TenantContext.getTenantId();
        bookService.batchMoveCategory(userId, dto.getBookIds(), dto.getCategoryId());
        return Result.success();
    }

    @GetMapping("/books/{id}/page/{pageNum}")
    @RateLimit(type = RateLimit.RateLimitType.USER, limit = 120, windowSeconds = 60)
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
        Integer lastChapter = body.get("lastChapter");
        if (lastPage != null && lastPage >= 0) {
            bookService.updateLastPage(userId, id, lastPage);
        }
        if (lastChapter != null && lastChapter >= 0) {
            bookService.updateLastChapter(userId, id, lastChapter);
        }
        if (lastPage == null && lastChapter == null) {
            throw new com.xiaoan.bookstore.exception.BusinessException("进度参数无效");
        }
        return Result.success();
    }

    @GetMapping("/books/{id}/chapter/{chapterIndex}")
    public Result<String> getChapterHtml(@PathVariable Long id,
                                          @PathVariable int chapterIndex) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.getChapterHtml(userId, id, chapterIndex));
    }

    @GetMapping("/books/{id}/stream-type")
    public Result<String> getStreamType(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.getStreamType(userId, id));
    }

    @GetMapping("/books/{id}/cover-thumbnail")
    public ResponseEntity<byte[]> getCoverThumbnail(@PathVariable Long id) {
        byte[] image = pdfPreRenderService.getCoverThumbnail(id);
        if (image == null || image.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @GetMapping("/books/{id}/cover-thumbnail/generate")
    public ResponseEntity<byte[]> generateAndGetThumbnail(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        byte[] image = pdfPreRenderService.generateAndGetThumbnail(userId, id);
        if (image == null || image.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @GetMapping("/reader/config")
    public Result<Map<String, Object>> getReaderConfig() {
        Map<String, Object> config = sysConfigService.getConfigMapByCategory("reader");
        config.putAll(sysConfigService.getConfigMapByCategory("pdf"));
        return Result.success(config);
    }

    @GetMapping("/books/{id}/unit/{unitIndex}")
    public Result<String> getUnitContent(@PathVariable Long id,
                                          @PathVariable int unitIndex) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.getUnitContent(userId, id, unitIndex));
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

    @GetMapping("/categories/{id}/book-count")
    public Result<Map<String, Object>> categoryBookCount(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        long count = categoryService.countBooks(userId, id);
        return Result.success(Map.of("count", count));
    }

    @PutMapping("/categories/sort")
    @Log("分类排序")
    public Result<Void> sortCategories(@RequestBody List<Map<String, Object>> sortList) {
        Long userId = TenantContext.getTenantId();
        categoryService.batchSort(userId, sortList);
        return Result.success();
    }

    @DeleteMapping("/categories/{id}")
    @Log("删除分类")
    public Result<Void> deleteCategory(@PathVariable Long id,
                                        @RequestParam(required = false, defaultValue = "false") boolean moveToUncategorized) {
        Long userId = TenantContext.getTenantId();
        categoryService.delete(userId, id, moveToUncategorized);
        return Result.success();
    }

    @GetMapping("/books/metadata/search")
    @RateLimit(type = RateLimit.RateLimitType.USER, limit = 30, windowSeconds = 60)
    public Result<com.xiaoan.bookstore.dto.BookMetadataVO> searchMetadata(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String isbn) {
        if ((title == null || title.isBlank())
                && (author == null || author.isBlank())
                && (isbn == null || isbn.isBlank())) {
            return Result.error(400, "请至少提供书名、作者或ISBN中的一项");
        }
        return Result.success(bookService.searchMetadata(title, author, isbn));
    }

    @GetMapping("/books/metadata/search-list")
    @RateLimit(type = RateLimit.RateLimitType.USER, limit = 20, windowSeconds = 60)
    public Result<java.util.List<com.xiaoan.bookstore.dto.BookMetadataVO>> searchMetadataList(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String isbn,
            @RequestParam(defaultValue = "10") int limit) {
        if ((title == null || title.isBlank())
                && (author == null || author.isBlank())
                && (isbn == null || isbn.isBlank())) {
            return Result.error(400, "请至少提供书名、作者或ISBN中的一项");
        }
        limit = Math.min(Math.max(limit, 1), 50);
        return Result.success(bookService.searchMetadataList(title, author, isbn, limit));
    }

    @PostMapping("/books/{id}/metadata/apply")
    @Log("应用书籍元数据")
    public Result<Book> applyMetadata(@PathVariable Long id,
                                       @RequestBody com.xiaoan.bookstore.dto.BookMetadataVO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bookService.applyMetadataToBook(userId, id, dto));
    }

    @PostMapping("/books/{id}/metadata/refresh")
    @Log("刷新书籍元数据")
    @RateLimit(type = RateLimit.RateLimitType.USER, limit = 5, windowSeconds = 60)
    public Result<Void> refreshMetadata(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        Book book = bookService.getBookById(userId, id);
        bookService.fetchAndApplyMetadata(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn());
        return Result.success();
    }
}
