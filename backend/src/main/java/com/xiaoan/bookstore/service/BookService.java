package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.BookMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
    private final BookMapper bookMapper;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Value("${app.upload.max-size}")
    private long maxFileSize;

    private static final long MIN_FILE_SIZE = 1024 * 1024;

    public Book upload(Long userId, MultipartFile file, String title, String author, Long categoryId) {
        if (file.isEmpty()) {
            throw new BusinessException("请选择文件");
        }
        if (file.getSize() < MIN_FILE_SIZE) {
            throw new BusinessException("文件大小不能小于1MB");
        }
        if (file.getSize() > maxFileSize) {
            throw new BusinessException("文件大小不能超过150MB");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException("仅支持PDF文件");
        }

        String fileName = UUID.randomUUID() + ".pdf";
        String userDir = uploadPath + File.separator + userId;
        Path dirPath = Paths.get(userDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dirPath);
            Path filePath = dirPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            int pageCount = 0;
            try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
                pageCount = doc.getNumberOfPages();
            }

            Book book = new Book();
            book.setUserId(userId);
            book.setTitle(title != null && !title.isEmpty() ? title : originalName.replace(".pdf", ""));
            book.setAuthor(author != null ? author : "");
            book.setFilePath(userId + "/" + fileName);
            book.setFileSize(file.getSize());
            book.setPageCount(pageCount);
            book.setCategoryId(categoryId);
            book.setLastPage(0);
            book.setStatus(Constants.STATUS_ENABLED);
            bookMapper.insert(book);

            log.info("书籍上传成功: userId={}, title={}, pages={}", userId, book.getTitle(), pageCount);
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

    public byte[] getPageImage(Long userId, Long bookId, int pageNum) {
        Book book = detail(userId, bookId);
        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();

        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            if (pageNum < 1 || pageNum > doc.getNumberOfPages()) {
                throw new BusinessException("页码超出范围");
            }
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage image = renderer.renderImageWithDPI(pageNum - 1, 150);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取PDF页面失败", e);
            throw new BusinessException("获取页面失败");
        }
    }

    public List<Map<String, Object>> getToc(Long userId, Long bookId) {
        Book book = detail(userId, bookId);
        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
        List<Map<String, Object>> toc = new ArrayList<>();

        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
            if (outline != null) {
                for (PDOutlineItem item : outline.children()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("title", item.getTitle());
                    int pageIndex = getPageNumber(doc, item);
                    entry.put("page", pageIndex + 1);
                    List<Map<String, Object>> children = new ArrayList<>();
                    for (PDOutlineItem child : item.children()) {
                        Map<String, Object> childEntry = new HashMap<>();
                        childEntry.put("title", child.getTitle());
                        childEntry.put("page", getPageNumber(doc, child) + 1);
                        children.add(childEntry);
                    }
                    if (!children.isEmpty()) {
                        entry.put("children", children);
                    }
                    toc.add(entry);
                }
            }
        } catch (Exception e) {
            log.error("获取PDF目录失败", e);
        }
        return toc;
    }

    private int getPageNumber(PDDocument doc, PDOutlineItem item) {
        try {
            PDPage page = item.findDestinationPage(doc);
            if (page != null) {
                return doc.getPages().indexOf(page);
            }
        } catch (Exception e) {
            log.debug("获取目录页码失败: {}", item.getTitle());
        }
        return 0;
    }

    public String getPageText(Long userId, Long bookId, int pageNum) {
        Book book = detail(userId, bookId);
        Path filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();

        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            if (pageNum < 1 || pageNum > doc.getNumberOfPages()) {
                throw new BusinessException("页码超出范围");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(pageNum);
            stripper.setEndPage(pageNum);
            return stripper.getText(doc);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取PDF文本失败", e);
            throw new BusinessException("获取文本失败");
        }
    }

    public void updateLastPage(Long userId, Long bookId, int lastPage) {
        Book book = detail(userId, bookId);
        book.setLastPage(lastPage);
        bookMapper.updateById(book);
    }
}
