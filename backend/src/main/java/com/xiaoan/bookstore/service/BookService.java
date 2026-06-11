package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.dto.BookDetailVO;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.entity.Category;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.AnnotationMapper;
import com.xiaoan.bookstore.mapper.BookMapper;
import com.xiaoan.bookstore.mapper.ReadingRecordMapper;
import com.xiaoan.bookstore.service.reader.ReaderAdapter;
import com.xiaoan.bookstore.service.reader.ReaderAdapterFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".epub", ".mobi", ".azw3");
    private static final long MIN_FILE_SIZE = 1024 * 1024;

    private final BookMapper bookMapper;
    private final AnnotationMapper annotationMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final ContentComplianceService contentComplianceService;
    private final MembershipService membershipService;
    private final PointsService pointsService;
    private final ReaderAdapterFactory readerAdapterFactory;
    private final BookIndexService bookIndexService;
    private final CategoryService categoryService;
    private final PdfPreRenderService pdfPreRenderService;
    private final SysConfigService sysConfigService;
    private final com.xiaoan.bookstore.service.reader.PdfReaderAdapter pdfReaderAdapter;

    @Value("${app.upload.path}")
    private String uploadPath;

    public Book upload(Long userId, MultipartFile file, String title, String author, Long categoryId, Integer copyrightDeclared) {
        if (file.isEmpty()) {
            throw new BusinessException("请选择文件");
        }
        if (file.getSize() < MIN_FILE_SIZE) {
            throw new BusinessException("文件大小不能小于1MB");
        }

        String originalName = file.getOriginalFilename();
        String format = ReaderAdapterFactory.resolveFormat(originalName);
        long maxFileSize = getMaxFileSize(format);
        if (file.getSize() > maxFileSize) {
            String maxText = format.equals(Constants.FORMAT_PDF) ? "150MB" : "100MB";
            throw new BusinessException(format.toUpperCase() + "文件大小不能超过" + maxText);
        }

        if (!isAllowedExtension(originalName)) {
            throw new BusinessException("仅支持PDF、EPUB、MOBI/AZW3格式文件");
        }

        membershipService.checkBookQuota(userId);
        membershipService.checkStorageQuota(userId, file.getSize());

        String extension = getExtension(originalName);
        String fileName = UUID.randomUUID() + extension;
        String userDir = uploadPath + File.separator + userId;
        Path dirPath = Paths.get(userDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dirPath);
            Path filePath = dirPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            ReaderAdapter adapter = readerAdapterFactory.getAdapter(format);
            int totalUnits = adapter.getTotalUnits(filePath.toString());
            int pageCount = 0;
            int chapterCount = 0;
            if (Constants.FORMAT_PDF.equals(format)) {
                pageCount = totalUnits;
            } else {
                chapterCount = totalUnits;
            }

            Map<String, String> meta = adapter.extractMetadata(filePath.toString());
            String metaTitle = meta.get("title");
            String metaAuthor = meta.get("author");

            Book book = new Book();
            book.setUserId(userId);
            String bookTitle;
            if (title != null && !title.isBlank()) {
                bookTitle = title;
            } else if (metaTitle != null && !metaTitle.isBlank()) {
                bookTitle = metaTitle;
            } else {
                bookTitle = originalName.replace(extension, "");
            }
            book.setTitle(bookTitle);

            String bookAuthor;
            if (author != null && !author.isBlank()) {
                bookAuthor = author;
            } else if (metaAuthor != null && !metaAuthor.isBlank()) {
                bookAuthor = metaAuthor;
            } else {
                bookAuthor = "";
            }
            book.setAuthor(bookAuthor);

            book.setFilePath(userId + "/" + fileName);
            book.setFileSize(file.getSize());
            book.setBookFormat(format);
            book.setPageCount(pageCount);
            book.setChapterCount(chapterCount);
            book.setCategoryId(categoryId);
            book.setLastPage(0);
            book.setLastChapter(0);
            book.setCopyrightDeclared(copyrightDeclared != null && copyrightDeclared == 1 ? 1 : 0);
            book.setStatus(Constants.STATUS_ENABLED);
            book.setCreatedAt(LocalDateTime.now());
            book.setUpdatedAt(LocalDateTime.now());
            bookMapper.insert(book);

            try {
                contentComplianceService.auditText(Constants.AUDIT_TARGET_BOOK_TITLE, book.getId(), bookTitle);
            } catch (Exception e) {
                log.warn("书名审核失败，不影响上传: {}", e.getMessage());
            }

            log.info("书籍上传成功: userId={}, title={}, author={}, format={}, pages={}, chapters={}, copyrightDeclared={}",
                    userId, bookTitle, bookAuthor, format, pageCount, chapterCount, book.getCopyrightDeclared());

            try {
                pointsService.earnPoints(userId, Constants.POINTS_CATEGORY_UPLOAD_BOOK, 0, "上传书籍《" + bookTitle + "》", String.valueOf(book.getId()));
            } catch (Exception e) {
                log.warn("积分发放失败，不影响上传: {}", e.getMessage());
            }

            if (Constants.FORMAT_PDF.equals(format) || Constants.FORMAT_EPUB.equals(format)) {
                try {
                    bookIndexService.createIndexTask(userId, book.getId());
                    bookIndexService.processIndexTask();
                } catch (Exception e) {
                    log.warn("创建索引任务失败，不影响上传: {}", e.getMessage());
                }
            }

            if (Constants.FORMAT_PDF.equals(format)) {
                try {
                    pdfPreRenderService.asyncPreRenderBook(book.getId());
                } catch (Exception e) {
                    log.warn("触发PDF预渲染失败，不影响上传: {}", e.getMessage());
                }
            }

            return book;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    public Page<Book> myBooks(Long userId, Long categoryId, int page, int size, String keyword, String sortBy) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, Constants.STATUS_ENABLED);
        wrapper.isNull(Book::getDeletedAt);
        wrapper.eq(Book::getUserId, userId);

        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }

        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.trim() + "%";
            wrapper.and(w -> w.like(Book::getTitle, kw).or().like(Book::getAuthor, kw));
        }

        switch (sortBy != null ? sortBy : Constants.BOOK_SORT_UPLOAD_TIME) {
            case Constants.BOOK_SORT_LAST_READ:
                wrapper.orderByDesc(Book::getLastReadAt);
                wrapper.orderByDesc(Book::getUpdatedAt);
                break;
            case Constants.BOOK_SORT_TITLE:
                wrapper.orderByAsc(Book::getTitle);
                break;
            case Constants.BOOK_SORT_UPLOAD_TIME:
            default:
                wrapper.orderByDesc(Book::getCreatedAt);
                break;
        }

        return bookMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public BookDetailVO detail(Long userId, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getStatus() != Constants.STATUS_ENABLED || book.getDeletedAt() != null) {
            throw new BusinessException("书籍不存在");
        }
        TenantValidator.validateCrossTenant(book.getUserId(), userId);
        return buildDetailVO(book);
    }

    public Book updateBook(Long userId, Long bookId, String title, String author, Long categoryId) {
        Book book = detailInternal(userId, bookId);
        boolean changed = false;
        if (title != null && !title.isBlank() && !title.equals(book.getTitle())) {
            book.setTitle(title);
            changed = true;
        }
        if (author != null && !author.equals(book.getAuthor())) {
            book.setAuthor(author);
            changed = true;
        }
        if (categoryId != null && !categoryId.equals(book.getCategoryId())) {
            if (categoryId > 0) {
                Category cat = categoryService.getById(userId, categoryId);
                if (cat == null) {
                    throw new BusinessException("分类不存在");
                }
            }
            book.setCategoryId(categoryId > 0 ? categoryId : null);
            changed = true;
        }
        if (changed) {
            book.setUpdatedAt(LocalDateTime.now());
            bookMapper.updateById(book);
            log.info("书籍信息已更新: bookId={}, title={}, author={}, categoryId={}", bookId, book.getTitle(), book.getAuthor(), book.getCategoryId());
        }
        return book;
    }

    @Transactional
    public void softDelete(Long userId, Long bookId) {
        Book book = detailInternal(userId, bookId);
        book.setDeletedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(book);
        log.info("书籍移入回收站: userId={}, bookId={}, title={}", userId, bookId, book.getTitle());
    }

    @Transactional
    public void restore(Long userId, Long bookId) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getId, bookId);
        wrapper.eq(Book::getUserId, userId);
        wrapper.isNotNull(Book::getDeletedAt);
        Book book = bookMapper.selectOne(wrapper);
        if (book == null) {
            throw new BusinessException("回收站中未找到该书籍");
        }
        LocalDateTime expireAt = book.getDeletedAt().plusDays(Constants.BOOK_TRASH_EXPIRE_DAYS);
        if (LocalDateTime.now().isAfter(expireAt)) {
            throw new BusinessException("该书籍已超过可恢复期限（7天）");
        }
        book.setDeletedAt(null);
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(book);
        log.info("书籍从回收站恢复: userId={}, bookId={}, title={}", userId, bookId, book.getTitle());
    }

    public Page<Book> trashList(Long userId, int page, int size) {
        LocalDateTime expireBefore = LocalDateTime.now().minusDays(Constants.BOOK_TRASH_EXPIRE_DAYS);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getUserId, userId);
        wrapper.isNotNull(Book::getDeletedAt);
        wrapper.ge(Book::getDeletedAt, expireBefore);
        wrapper.orderByDesc(Book::getDeletedAt);
        return bookMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Scheduled(cron = "0 15 3 * * ?")
    @Transactional
    public void cleanupExpiredTrash() {
        LocalDateTime expireBefore = LocalDateTime.now().minusDays(Constants.BOOK_TRASH_EXPIRE_DAYS);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Book::getDeletedAt);
        wrapper.lt(Book::getDeletedAt, expireBefore);
        List<Book> expired = bookMapper.selectList(wrapper);
        if (expired != null && !expired.isEmpty()) {
            for (Book book : expired) {
                try {
                    book.setStatus(Constants.STATUS_DISABLED);
                    book.setUpdatedAt(LocalDateTime.now());
                    bookMapper.updateById(book);
                    log.info("回收站书籍过期已永久删除: bookId={}, title={}", book.getId(), book.getTitle());
                } catch (Exception e) {
                    log.error("清理过期回收站书籍失败: bookId={}", book.getId(), e);
                }
            }
        }
    }

    @Transactional
    public void batchDelete(Long userId, List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            throw new BusinessException("请选择要删除的书籍");
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long bookId : bookIds) {
            try {
                Book book = detailInternal(userId, bookId);
                book.setDeletedAt(now);
                book.setUpdatedAt(now);
                bookMapper.updateById(book);
            } catch (Exception e) {
                log.warn("批量删除单本失败: userId={}, bookId={}", userId, bookId, e);
            }
        }
        log.info("批量删除完成: userId={}, count={}", userId, bookIds.size());
    }

    @Transactional
    public void batchMoveCategory(Long userId, List<Long> bookIds, Long categoryId) {
        if (bookIds == null || bookIds.isEmpty()) {
            throw new BusinessException("请选择要移动的书籍");
        }
        if (categoryId != null && categoryId > 0) {
            Category cat = categoryService.getById(userId, categoryId);
            if (cat == null) {
                throw new BusinessException("分类不存在");
            }
        }
        Long finalCatId = (categoryId != null && categoryId > 0) ? categoryId : null;
        LocalDateTime now = LocalDateTime.now();
        for (Long bookId : bookIds) {
            try {
                Book book = detailInternal(userId, bookId);
                book.setCategoryId(finalCatId);
                book.setUpdatedAt(now);
                bookMapper.updateById(book);
            } catch (Exception e) {
                log.warn("批量移动分类单本失败: userId={}, bookId={}", userId, bookId, e);
            }
        }
        log.info("批量移动分类完成: userId={}, count={}, categoryId={}", userId, bookIds.size(), categoryId);
    }

    public void delete(Long userId, Long bookId) {
        softDelete(userId, bookId);
    }

    public String getStreamType(Long userId, Long bookId) {
        Book book = detailInternal(userId, bookId);
        ReaderAdapter adapter = readerAdapterFactory.getAdapter(book.getBookFormat());
        return adapter.getStreamType();
    }

    public byte[] getPageImage(Long userId, Long bookId, int pageNum) {
        Book book = detailInternal(userId, bookId);

        byte[] cachedImage = pdfPreRenderService.getCachedPageImage(userId, bookId, pageNum);
        if (cachedImage != null && cachedImage.length > 0) {
            return cachedImage;
        }

        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();

        if (Constants.FORMAT_PDF.equals(book.getBookFormat())) {
            int dpi = sysConfigService.getPdfRenderDpi();
            byte[] result = pdfReaderAdapter.getUnitImageWithDpi(filePath.toString(), pageNum, dpi);
            if (result == null || result.length == 0) {
                throw new BusinessException("该格式不支持图片渲染");
            }
            return result;
        }

        ReaderAdapter adapter = readerAdapterFactory.getAdapter(book.getBookFormat());
        byte[] result = adapter.getUnitImage(filePath.toString(), pageNum);
        if (result == null || result.length == 0) {
            throw new BusinessException("该格式不支持图片渲染");
        }
        return result;
    }

    public List<Map<String, Object>> getToc(Long userId, Long bookId) {
        Book book = detailInternal(userId, bookId);
        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
        ReaderAdapter adapter = readerAdapterFactory.getAdapter(book.getBookFormat());
        return adapter.getToc(filePath.toString());
    }

    public String getPageText(Long userId, Long bookId, int pageNum) {
        Book book = detailInternal(userId, bookId);
        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
        ReaderAdapter adapter = readerAdapterFactory.getAdapter(book.getBookFormat());
        return adapter.getUnitContent(filePath.toString(), pageNum);
    }

    public String getUnitContent(Long userId, Long bookId, int unitIndex) {
        Book book = detailInternal(userId, bookId);
        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
        ReaderAdapter adapter = readerAdapterFactory.getAdapter(book.getBookFormat());
        return adapter.getUnitContent(filePath.toString(), unitIndex);
    }

    public String getChapterHtml(Long userId, Long bookId, int chapterIndex) {
        Book book = detailInternal(userId, bookId);
        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
        ReaderAdapter adapter = readerAdapterFactory.getAdapter(book.getBookFormat());
        return adapter.getUnitContent(filePath.toString(), chapterIndex);
    }

    public void updateLastPage(Long userId, Long bookId, int lastPage) {
        Book book = detailInternal(userId, bookId);
        book.setLastPage(lastPage);
        book.setLastReadAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(book);
    }

    public void updateLastChapter(Long userId, Long bookId, int lastChapter) {
        Book book = detailInternal(userId, bookId);
        book.setLastChapter(lastChapter);
        book.setLastReadAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(book);
    }

    public void update(Book book) {
        bookMapper.updateById(book);
    }

    public Book getBookById(Long userId, Long bookId) {
        return detailInternal(userId, bookId);
    }

    private Book detailInternal(Long userId, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getStatus() != Constants.STATUS_ENABLED || book.getDeletedAt() != null) {
            throw new BusinessException("书籍不存在");
        }
        TenantValidator.validateCrossTenant(book.getUserId(), TenantContext.getTenantId());
        return book;
    }

    private BookDetailVO buildDetailVO(Book book) {
        BookDetailVO vo = new BookDetailVO();
        vo.setId(book.getId());
        vo.setUserId(book.getUserId());
        vo.setTitle(book.getTitle());
        vo.setAuthor(book.getAuthor());
        vo.setFilePath(book.getFilePath());
        vo.setFileSize(book.getFileSize());
        vo.setFileSizeText(formatSize(book.getFileSize()));
        vo.setBookFormat(book.getBookFormat());
        vo.setPageCount(book.getPageCount());
        vo.setChapterCount(book.getChapterCount());
        vo.setCategoryId(book.getCategoryId());
        vo.setLastPage(book.getLastPage());
        vo.setLastChapter(book.getLastChapter());
        vo.setCopyrightDeclared(book.getCopyrightDeclared());
        vo.setStatus(book.getStatus());

        if (book.getCategoryId() != null) {
            Category cat = categoryService.getById(book.getUserId(), book.getCategoryId());
            if (cat != null) {
                vo.setCategoryName(cat.getName());
                vo.setCategoryColor(cat.getColor());
            }
        }

        int progress = 0;
        if (Constants.FORMAT_EPUB.equals(book.getBookFormat())) {
            int total = Math.max(1, book.getChapterCount() != null ? book.getChapterCount() : 0);
            int last = book.getLastChapter() != null ? book.getLastChapter() : 0;
            progress = Math.min(100, Math.round((last * 100.0f) / total));
        } else {
            int total = Math.max(1, book.getPageCount() != null ? book.getPageCount() : 0);
            int last = book.getLastPage() != null ? book.getLastPage() : 0;
            progress = Math.min(100, Math.round((last * 100.0f) / total));
        }
        vo.setProgressPercent(progress);

        Integer annCount = annotationMapper.countByBookId(book.getUserId(), book.getId());
        vo.setAnnotationCount(annCount != null ? annCount : 0);

        Long duration = readingRecordMapper.sumDurationByBookId(book.getUserId(), book.getId());
        long totalDuration = duration != null ? duration : 0L;
        vo.setTotalDuration(totalDuration);
        vo.setTotalDurationText(formatDuration(totalDuration));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (book.getCreatedAt() != null) vo.setCreatedAt(book.getCreatedAt().format(dtf));
        if (book.getUpdatedAt() != null) vo.setUpdatedAt(book.getUpdatedAt().format(dtf));
        if (book.getLastReadAt() != null) vo.setLastReadAt(book.getLastReadAt().format(dtf));

        return vo;
    }

    private String formatSize(Long bytes) {
        if (bytes == null || bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "0分钟";
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        } else {
            return minutes + "分钟";
        }
    }

    private boolean isAllowedExtension(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private String getExtension(String filename) {
        if (filename == null) return ".pdf";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".azw3")) return ".azw3";
        if (lower.endsWith(".mobi")) return ".mobi";
        if (lower.endsWith(".epub")) return ".epub";
        return ".pdf";
    }

    private long getMaxFileSize(String format) {
        return switch (format) {
            case Constants.FORMAT_EPUB -> Constants.MAX_SIZE_EPUB;
            case Constants.FORMAT_MOBI -> Constants.MAX_SIZE_MOBI;
            default -> Constants.MAX_SIZE_PDF;
        };
    }
}
