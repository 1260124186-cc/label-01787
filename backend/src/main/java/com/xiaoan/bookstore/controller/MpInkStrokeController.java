package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.InkStrokeDTO;
import com.xiaoan.bookstore.entity.InkStroke;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.service.BookService;
import com.xiaoan.bookstore.service.InkExportService;
import com.xiaoan.bookstore.service.InkStrokeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mp/ink")
@RequiredArgsConstructor
public class MpInkStrokeController {

    private final InkStrokeService inkStrokeService;
    private final InkExportService inkExportService;
    private final BookService bookService;

    @Value("${app.upload.path}")
    private String uploadPath;

    @PostMapping("/stroke")
    @Log("保存单条墨迹笔迹")
    public Result<InkStroke> saveStroke(@Valid @RequestBody InkStrokeDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(inkStrokeService.saveStroke(userId, dto));
    }

    @PostMapping("/batch")
    @Log("批量同步墨迹")
    public Result<Map<String, Object>> batchSync(@RequestBody Map<String, Object> body) {
        Long userId = TenantContext.getTenantId();
        Long bookId = Long.valueOf(body.get("bookId").toString());
        Integer pageNum = Integer.valueOf(body.get("pageNum").toString());
        List<InkStrokeDTO> strokes = (List<InkStrokeDTO>) body.get("strokes");
        List<String> deletedStrokeIds = (List<String>) body.get("deletedStrokeIds");
        return Result.success(inkStrokeService.batchSync(userId, bookId, pageNum, strokes, deletedStrokeIds));
    }

    @GetMapping("/page/{bookId}/{pageNum}")
    public Result<List<InkStroke>> getByPage(@PathVariable Long bookId, @PathVariable Integer pageNum) {
        Long userId = TenantContext.getTenantId();
        return Result.success(inkStrokeService.getByPage(userId, bookId, pageNum));
    }

    @GetMapping("/book/{bookId}")
    public Result<List<InkStroke>> getByBook(@PathVariable Long bookId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(inkStrokeService.getByBook(userId, bookId));
    }

    @PostMapping("/book/pages")
    public Result<Map<Integer, List<InkStroke>>> getByBookAndPages(@RequestBody Map<String, Object> body) {
        Long userId = TenantContext.getTenantId();
        Long bookId = Long.valueOf(body.get("bookId").toString());
        List<Integer> pageNums = (List<Integer>) body.get("pageNums");
        List<InkStroke> strokes = inkStrokeService.getByBookAndPages(userId, bookId, pageNums);
        return Result.success(inkStrokeService.groupByPage(strokes));
    }

    @GetMapping("/stats/{bookId}")
    public Result<List<Map<String, Object>>> getPageStats(@PathVariable Long bookId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(inkStrokeService.getPageStats(userId, bookId));
    }

    @DeleteMapping("/stroke/{id}")
    @Log("删除墨迹笔迹")
    public Result<Void> deleteStroke(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        inkStrokeService.deleteStroke(userId, id);
        return Result.success();
    }

    @DeleteMapping("/stroke")
    @Log("按strokeId删除墨迹")
    public Result<Void> deleteByStrokeId(@RequestParam Long bookId, @RequestParam String strokeId) {
        Long userId = TenantContext.getTenantId();
        inkStrokeService.deleteByStrokeId(userId, bookId, strokeId);
        return Result.success();
    }

    @DeleteMapping("/page/{bookId}/{pageNum}")
    @Log("清空页面墨迹")
    public Result<Void> clearPage(@PathVariable Long bookId, @PathVariable Integer pageNum) {
        Long userId = TenantContext.getTenantId();
        inkStrokeService.clearPage(userId, bookId, pageNum);
        return Result.success();
    }

    @DeleteMapping("/book/{bookId}")
    @Log("清空书籍墨迹")
    public Result<Void> clearBook(@PathVariable Long bookId) {
        Long userId = TenantContext.getTenantId();
        inkStrokeService.clearBook(userId, bookId);
        return Result.success();
    }

    @GetMapping("/export/page/{bookId}/{pageNum}")
    @Log("导出单页带墨迹图片")
    public ResponseEntity<byte[]> exportPageWithInk(
            @PathVariable Long bookId,
            @PathVariable Integer pageNum,
            @RequestParam(defaultValue = "image") String format) {
        Long userId = TenantContext.getTenantId();
        String filePath = getBookFilePath(userId, bookId);

        byte[] data;
        String fileName;
        MediaType mediaType;

        if ("pdf".equalsIgnoreCase(format)) {
            data = inkExportService.exportInkToPdfOverlay(userId, bookId, filePath, List.of(pageNum));
            fileName = "book_" + bookId + "_page_" + pageNum + "_ink.pdf";
            mediaType = MediaType.APPLICATION_PDF;
        } else {
            data = inkExportService.exportPageWithInk(userId, bookId, pageNum, filePath);
            fileName = "book_" + bookId + "_page_" + pageNum + "_ink.png";
            mediaType = MediaType.IMAGE_PNG;
        }

        return buildDownloadResponse(data, fileName, mediaType);
    }

    @PostMapping("/export/pages")
    @Log("导出多页带墨迹")
    public ResponseEntity<byte[]> exportPagesWithInk(@RequestBody Map<String, Object> body) {
        Long userId = TenantContext.getTenantId();
        Long bookId = Long.valueOf(body.get("bookId").toString());
        List<Integer> pageNums = (List<Integer>) body.get("pageNums");
        String format = body.getOrDefault("format", "pdf").toString();
        boolean overlay = "true".equalsIgnoreCase(body.getOrDefault("overlay", "true").toString());

        String filePath = getBookFilePath(userId, bookId);
        byte[] data;
        String fileName;
        MediaType mediaType;

        if ("image".equalsIgnoreCase(format)) {
            if (pageNums != null && pageNums.size() == 1) {
                data = inkExportService.exportPageWithInk(userId, bookId, pageNums.get(0), filePath);
                fileName = "book_" + bookId + "_page_" + pageNums.get(0) + "_ink.png";
                mediaType = MediaType.IMAGE_PNG;
            } else {
                data = inkExportService.exportPagesWithInk(userId, bookId, pageNums, filePath);
                fileName = "book_" + bookId + "_pages_ink.pdf";
                mediaType = MediaType.APPLICATION_PDF;
            }
        } else {
            if (overlay) {
                data = inkExportService.exportInkToPdfOverlay(userId, bookId, filePath, pageNums);
            } else {
                data = inkExportService.exportPagesWithInk(userId, bookId, pageNums, filePath);
            }
            fileName = "book_" + bookId + "_ink.pdf";
            mediaType = MediaType.APPLICATION_PDF;
        }

        return buildDownloadResponse(data, fileName, mediaType);
    }

    @GetMapping("/export/book/{bookId}")
    @Log("导出全书带墨迹PDF")
    public ResponseEntity<byte[]> exportBookWithInk(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "true") boolean overlay) {
        Long userId = TenantContext.getTenantId();
        String filePath = getBookFilePath(userId, bookId);

        byte[] data;
        if (overlay) {
            data = inkExportService.exportInkToPdfOverlay(userId, bookId, filePath, null);
        } else {
            data = inkExportService.exportBookWithInk(userId, bookId, filePath);
        }

        String fileName = "book_" + bookId + "_with_ink.pdf";
        return buildDownloadResponse(data, fileName, MediaType.APPLICATION_PDF);
    }

    @GetMapping("/export/ink-only/{bookId}/{pageNum}")
    @Log("导出墨迹图层图片")
    public ResponseEntity<byte[]> exportInkOnly(
            @PathVariable Long bookId,
            @PathVariable Integer pageNum,
            @RequestParam(defaultValue = "1000") int width,
            @RequestParam(defaultValue = "1400") int height) {
        Long userId = TenantContext.getTenantId();
        byte[] data = inkExportService.exportInkOnlyAsImage(userId, bookId, pageNum, width, height);
        String fileName = "book_" + bookId + "_page_" + pageNum + "_ink_only.png";
        return buildDownloadResponse(data, fileName, MediaType.IMAGE_PNG);
    }

    private String getBookFilePath(Long userId, Long bookId) {
        var book = bookService.getBookById(userId, bookId);
        if (!Constants.FORMAT_PDF.equals(book.getBookFormat())) {
            throw new BusinessException("仅支持PDF格式的墨迹导出");
        }
        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
        return filePath.toString();
    }

    private ResponseEntity<byte[]> buildDownloadResponse(byte[] data, String fileName, MediaType mediaType) {
        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
        } catch (Exception e) {
            encodedFileName = fileName;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .contentType(mediaType)
                .contentLength(data.length)
                .body(data);
    }
}
