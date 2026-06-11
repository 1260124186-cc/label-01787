package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.mapper.BookMapper;
import com.xiaoan.bookstore.service.reader.PdfReaderAdapter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class PdfPreRenderService {

    private static final Logger log = LoggerFactory.getLogger(PdfPreRenderService.class);
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PROCESSING = 1;
    private static final int STATUS_COMPLETED = 2;
    private static final int STATUS_FAILED = 3;

    private final BookMapper bookMapper;
    private final PdfReaderAdapter pdfReaderAdapter;
    private final SysConfigService sysConfigService;

    @Value("${app.upload.path}")
    private String uploadPath;

    private final ConcurrentHashMap<Long, ReentrantLock> bookLocks = new ConcurrentHashMap<>();

    @Async
    public void asyncPreRenderBook(Long bookId) {
        if (!sysConfigService.isPdfPrerenderEnabled()) {
            log.debug("PDF预渲染已禁用，跳过 bookId={}", bookId);
            return;
        }

        ReentrantLock lock = bookLocks.computeIfAbsent(bookId, k -> new ReentrantLock());
        if (lock.isLocked()) {
            log.warn("该书籍正在预渲染中，跳过重复请求: bookId={}", bookId);
            return;
        }

        try {
            lock.lock();
            doPreRender(bookId);
        } finally {
            lock.unlock();
            bookLocks.remove(bookId);
        }
    }

    @Transactional
    public void doPreRender(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            log.error("书籍不存在: bookId={}", bookId);
            return;
        }

        if (!Constants.FORMAT_PDF.equals(book.getBookFormat())) {
            log.debug("非PDF格式，跳过预渲染: bookId={}, format={}", bookId, book.getBookFormat());
            return;
        }

        try {
            updateBookStatus(bookId, STATUS_PROCESSING, 0, null);

            Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
            String baseDir = filePath.getParent().toString();
            String bookFileName = filePath.getFileName().toString();
            String cacheDir = baseDir + "/cache_" + bookFileName.replace(".pdf", "");

            int renderDpi = sysConfigService.getPdfRenderDpi();
            int thumbnailDpi = sysConfigService.getPdfThumbnailDpi();
            int preRenderPages = sysConfigService.getPdfPrerenderPages();

            log.info("开始预渲染PDF: bookId={}, renderDpi={}, thumbnailDpi={}, preRenderPages={}", 
                    bookId, renderDpi, thumbnailDpi, preRenderPages);

            String thumbnailPath = pdfReaderAdapter.saveThumbnailToFile(
                    filePath.toString(), 1, thumbnailDpi, 
                    cacheDir, "thumbnail"
            );

            if (thumbnailPath != null) {
                Path uploadPathObj = Paths.get(uploadPath).toAbsolutePath().normalize();
                Path thumbnailPathObj = Paths.get(thumbnailPath).toAbsolutePath().normalize();
                String relativeThumbnailPath = uploadPathObj.relativize(thumbnailPathObj).toString();
                book.setCoverThumbnail(relativeThumbnailPath);
                log.info("封面缩略图已生成: bookId={}, path={}", bookId, relativeThumbnailPath);
            }

            int totalPages = book.getPageCount() != null ? book.getPageCount() : 0;
            int endPage = Math.min(preRenderPages, totalPages);
            if (endPage > 0) {
                boolean success = pdfReaderAdapter.preRenderPages(
                        filePath.toString(), 1, endPage, renderDpi, cacheDir
                );
                if (success) {
                    updateBookStatus(bookId, STATUS_COMPLETED, endPage, null);
                    log.info("PDF预渲染完成: bookId={}, renderedPages={}", bookId, endPage);
                } else {
                    updateBookStatus(bookId, STATUS_FAILED, 0, "预渲染页面失败");
                    log.error("PDF预渲染失败: bookId={}", bookId);
                }
            } else {
                updateBookStatus(bookId, STATUS_COMPLETED, 0, null);
                log.info("PDF无页面需要预渲染: bookId={}", bookId);
            }

            if (book.getCoverThumbnail() != null || thumbnailPath != null) {
                bookMapper.updateById(book);
            }

        } catch (Exception e) {
            log.error("PDF预渲染异常: bookId={}", bookId, e);
            updateBookStatus(bookId, STATUS_FAILED, 0, e.getMessage());
        }
    }

    @Transactional
    public void updateBookStatus(Long bookId, int status, int renderedPages, String error) {
        try {
            Book book = new Book();
            book.setId(bookId);
            book.setPreRenderStatus(status);
            book.setPreRenderedPages(renderedPages);
            book.setPreRenderError(error);
            bookMapper.updateById(book);
        } catch (Exception e) {
            log.error("更新书籍预渲染状态失败: bookId={}", bookId, e);
        }
    }

    public byte[] getCachedPageImage(Long userId, Long bookId, int pageNum) {
        if (!sysConfigService.isPdfCacheEnabled()) {
            return null;
        }

        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return null;
        }

        try {
            Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
            String baseDir = filePath.getParent().toString();
            String bookFileName = filePath.getFileName().toString();
            String cacheDir = baseDir + "/cache_" + bookFileName.replace(".pdf", "");
            Path cachedPage = Paths.get(cacheDir, "page_" + pageNum + ".png");

            if (java.nio.file.Files.exists(cachedPage)) {
                log.debug("使用缓存页面: bookId={}, page={}", bookId, pageNum);
                return java.nio.file.Files.readAllBytes(cachedPage);
            }
        } catch (Exception e) {
            log.debug("读取缓存页面失败: bookId={}, page={}", bookId, pageNum, e);
        }
        return null;
    }

    public byte[] getCoverThumbnail(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getCoverThumbnail() == null || book.getCoverThumbnail().isBlank()) {
            return null;
        }

        try {
            Path thumbnailPath = Paths.get(uploadPath, book.getCoverThumbnail()).toAbsolutePath().normalize();
            if (java.nio.file.Files.exists(thumbnailPath)) {
                return java.nio.file.Files.readAllBytes(thumbnailPath);
            }
        } catch (Exception e) {
            log.debug("读取封面缩略图失败: bookId={}", bookId, e);
        }
        return null;
    }

    public byte[] generateAndGetThumbnail(Long userId, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return null;
        }

        if (!Constants.FORMAT_PDF.equals(book.getBookFormat())) {
            return null;
        }

        byte[] existing = getCoverThumbnail(bookId);
        if (existing != null && existing.length > 0) {
            return existing;
        }

        try {
            Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
            int thumbnailDpi = sysConfigService.getPdfThumbnailDpi();
            byte[] thumbnail = pdfReaderAdapter.generateThumbnail(filePath.toString(), 1, thumbnailDpi);

            if (thumbnail != null && thumbnail.length > 0) {
                String baseDir = filePath.getParent().toString();
                String bookFileName = filePath.getFileName().toString();
                String cacheDir = baseDir + "/cache_" + bookFileName.replace(".pdf", "");
                
                String savedPath = pdfReaderAdapter.saveThumbnailToFile(
                        filePath.toString(), 1, thumbnailDpi, cacheDir, "thumbnail"
                );

                if (savedPath != null) {
                    Path uploadPathObj = Paths.get(uploadPath).toAbsolutePath().normalize();
                    Path thumbnailPathObj = Paths.get(savedPath).toAbsolutePath().normalize();
                    String relativeThumbnailPath = uploadPathObj.relativize(thumbnailPathObj).toString();
                    book.setCoverThumbnail(relativeThumbnailPath);
                    bookMapper.updateById(book);
                }
            }
            return thumbnail;
        } catch (Exception e) {
            log.error("生成缩略图失败: bookId={}", bookId, e);
            return null;
        }
    }

    @Scheduled(cron = "0 30 4 * * ?")
    @Transactional
    public void retryFailedPreRender() {
        log.info("开始重试失败的PDF预渲染任务");
        try {
            LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Book::getBookFormat, Constants.FORMAT_PDF);
            wrapper.eq(Book::getPreRenderStatus, STATUS_FAILED);
            wrapper.isNull(Book::getDeletedAt);
            List<Book> failedBooks = bookMapper.selectList(wrapper);

            for (Book book : failedBooks) {
                try {
                    log.info("重试预渲染: bookId={}, title={}", book.getId(), book.getTitle());
                    asyncPreRenderBook(book.getId());
                } catch (Exception e) {
                    log.error("重试预渲染失败: bookId={}", book.getId(), e);
                }
            }
            log.info("重试失败的PDF预渲染任务完成，共处理 {} 本", failedBooks.size());
        } catch (Exception e) {
            log.error("重试预渲染任务异常", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void processPendingPreRender() {
        log.info("开始处理待预渲染的PDF书籍");
        try {
            LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Book::getBookFormat, Constants.FORMAT_PDF);
            wrapper.and(w -> w.isNull(Book::getPreRenderStatus).or().eq(Book::getPreRenderStatus, STATUS_PENDING));
            wrapper.isNull(Book::getDeletedAt);
            wrapper.orderByDesc(Book::getCreatedAt);
            wrapper.last("LIMIT 50");
            List<Book> pendingBooks = bookMapper.selectList(wrapper);

            for (Book book : pendingBooks) {
                try {
                    log.info("处理待预渲染书籍: bookId={}, title={}", book.getId(), book.getTitle());
                    asyncPreRenderBook(book.getId());
                } catch (Exception e) {
                    log.error("处理待预渲染书籍失败: bookId={}", book.getId(), e);
                }
            }
            log.info("处理待预渲染的PDF书籍完成，共处理 {} 本", pendingBooks.size());
        } catch (Exception e) {
            log.error("处理待预渲染任务异常", e);
        }
    }
}
