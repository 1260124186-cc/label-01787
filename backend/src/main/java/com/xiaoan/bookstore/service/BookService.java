package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.BookMapper;
import com.xiaoan.bookstore.service.reader.ReaderAdapter;
import com.xiaoan.bookstore.service.reader.ReaderAdapterFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".epub", ".mobi", ".azw3");
    private static final long MIN_FILE_SIZE = 1024 * 1024;

    private final BookMapper bookMapper;
    private final ContentComplianceService contentComplianceService;
    private final MembershipService membershipService;
    private final PointsService pointsService;
    private final ReaderAdapterFactory readerAdapterFactory;
    private final BookIndexService bookIndexService;

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

            int pageCount = 0;
            int chapterCount = 0;
            ReaderAdapter adapter = readerAdapterFactory.getAdapter(format);
            int totalUnits = adapter.getTotalUnits(filePath.toString());
            if (Constants.FORMAT_PDF.equals(format)) {
                pageCount = totalUnits;
            } else {
                chapterCount = totalUnits;
            }

            Book book = new Book();
            book.setUserId(userId);
            String bookTitle = title != null && !title.isBlank() ? title : originalName.replace(extension, "");
            book.setTitle(bookTitle);
            book.setAuthor(author != null ? author : "");
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
            bookMapper.insert(book);

            try {
                contentComplianceService.auditText(Constants.AUDIT_TARGET_BOOK_TITLE, book.getId(), bookTitle);
            } catch (Exception e) {
                log.warn("书名审核失败，不影响上传: {}", e.getMessage());
            }

            log.info("书籍上传成功: userId={}, title={}, format={}, pages={}, chapters={}, copyrightDeclared={}",
                    userId, bookTitle, format, pageCount, chapterCount, book.getCopyrightDeclared());

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

            return book;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    public Page<Book> myBooks(Long userId, Long categoryId, int page, int size) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, Constants.STATUS_ENABLED);
        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Book::getUpdatedAt);
        return bookMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Book detail(Long userId, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException("书籍不存在");
        }
        TenantValidator.validateCrossTenant(book.getUserId(), TenantContext.getTenantId());
        return book;
    }

    public void delete(Long userId, Long bookId) {
        Book book = detail(userId, bookId);
        book.setStatus(0);
        bookMapper.updateById(book);
        log.info("书籍删除: userId={}, bookId={}", userId, bookId);
    }

    public String getStreamType(Long userId, Long bookId) {
        Book book = detail(userId, bookId);
        ReaderAdapter adapter = readerAdapterFactory.getAdapter(book.getBookFormat());
        return adapter.getStreamType();
    }

    public byte[] getPageImage(Long userId, Long bookId, int pageNum) {
        Book book = detail(userId, bookId);
        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
        ReaderAdapter adapter = readerAdapterFactory.getAdapter(book.getBookFormat());
        byte[] result = adapter.getUnitImage(filePath.toString(), pageNum);
        if (result == null || result.length == 0) {
            throw new BusinessException("该格式不支持图片渲染");
        }
        return result;
    }

    public List<Map<String, Object>> getToc(Long userId, Long bookId) {
        Book book = detail(userId, bookId);
        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
        ReaderAdapter adapter = readerAdapterFactory.getAdapter(book.getBookFormat());
        return adapter.getToc(filePath.toString());
    }

    public String getPageText(Long userId, Long bookId, int pageNum) {
        Book book = detail(userId, bookId);
        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
        ReaderAdapter adapter = readerAdapterFactory.getAdapter(book.getBookFormat());
        return adapter.getUnitContent(filePath.toString(), pageNum);
    }

    public String getUnitContent(Long userId, Long bookId, int unitIndex) {
        Book book = detail(userId, bookId);
        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
        ReaderAdapter adapter = readerAdapterFactory.getAdapter(book.getBookFormat());
        return adapter.getUnitContent(filePath.toString(), unitIndex);
    }

    public String getChapterHtml(Long userId, Long bookId, int chapterIndex) {
        Book book = detail(userId, bookId);
        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
        ReaderAdapter adapter = readerAdapterFactory.getAdapter(book.getBookFormat());
        return adapter.getUnitContent(filePath.toString(), chapterIndex);
    }

    public void updateLastPage(Long userId, Long bookId, int lastPage) {
        Book book = detail(userId, bookId);
        book.setLastPage(lastPage);
        bookMapper.updateById(book);
    }

    public void updateLastChapter(Long userId, Long bookId, int lastChapter) {
        Book book = detail(userId, bookId);
        book.setLastChapter(lastChapter);
        bookMapper.updateById(book);
    }

    public void update(Book book) {
        bookMapper.updateById(book);
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
