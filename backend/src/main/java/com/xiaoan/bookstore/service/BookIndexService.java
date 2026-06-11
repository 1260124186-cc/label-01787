package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.IndexStatusVO;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.entity.BookIndexTask;
import com.xiaoan.bookstore.entity.BookPageText;
import com.xiaoan.bookstore.entity.User;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.BookIndexTaskMapper;
import com.xiaoan.bookstore.mapper.BookPageTextMapper;
import com.xiaoan.bookstore.mapper.BookMapper;
import com.xiaoan.bookstore.mapper.UserMapper;
import com.xiaoan.bookstore.service.reader.ReaderAdapter;
import com.xiaoan.bookstore.service.reader.ReaderAdapterFactory;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookIndexService {

    private static final Logger log = LoggerFactory.getLogger(BookIndexService.class);

    private final BookMapper bookMapper;
    private final BookPageTextMapper bookPageTextMapper;
    private final BookIndexTaskMapper bookIndexTaskMapper;
    private final UserMapper userMapper;
    private final ReaderAdapterFactory readerAdapterFactory;
    private final MembershipService membershipService;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Transactional
    public void createIndexTask(Long userId, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("书籍不存在");
        }

        int priority = membershipService.isVip(userId) ? 10 : 1;

        BookIndexTask existingTask = bookIndexTaskMapper.selectOne(
                new LambdaQueryWrapper<BookIndexTask>().eq(BookIndexTask::getBookId, bookId));

        if (existingTask != null) {
            existingTask.setStatus(Constants.INDEX_STATUS_PENDING);
            existingTask.setIndexedPages(0);
            existingTask.setErrorMessage("");
            existingTask.setStartedAt(null);
            existingTask.setFinishedAt(null);
            existingTask.setPriority(priority);
            bookIndexTaskMapper.updateById(existingTask);
            log.info("重新创建索引任务: bookId={}, userId={}", bookId, userId);
        } else {
            BookIndexTask task = new BookIndexTask();
            task.setUserId(userId);
            task.setBookId(bookId);
            task.setBookTitle(book.getTitle());
            task.setTotalPages(book.getPageCount());
            task.setIndexedPages(0);
            task.setStatus(Constants.INDEX_STATUS_PENDING);
            task.setPriority(priority);
            bookIndexTaskMapper.insert(task);
            log.info("创建索引任务: bookId={}, userId={}, title={}", bookId, userId, book.getTitle());
        }
    }

    @Async("indexTaskExecutor")
    public void processIndexTask() {
        BookIndexTask task = bookIndexTaskMapper.pickNextTask(
                Constants.INDEX_STATUS_PENDING, Constants.INDEX_STATUS_FAILED);

        if (task == null) {
            return;
        }

        log.info("开始处理索引任务: taskId={}, bookId={}, title={}",
                task.getId(), task.getBookId(), task.getBookTitle());

        try {
            bookIndexTaskMapper.updateStatusAndStart(
                    task.getId(), Constants.INDEX_STATUS_PROCESSING, LocalDateTime.now());

            Book book = bookMapper.selectById(task.getBookId());
            if (book == null) {
                throw new BusinessException("书籍不存在");
            }

            String filePath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize().toString();
            String format = book.getBookFormat();

            if (Constants.FORMAT_PDF.equals(format)) {
                indexPdfBook(task, book, filePath);
            } else if (Constants.FORMAT_EPUB.equals(format)) {
                indexEpubBook(task, book, filePath);
            } else {
                throw new BusinessException("不支持的格式: " + format);
            }

            bookIndexTaskMapper.updateStatusAndProgress(
                    task.getId(), Constants.INDEX_STATUS_COMPLETED,
                    task.getTotalPages(), LocalDateTime.now(), "");

            log.info("索引任务完成: taskId={}, bookId={}, pages={}",
                    task.getId(), task.getBookId(), task.getTotalPages());

        } catch (Exception e) {
            log.error("索引任务失败: taskId={}, bookId={}", task.getId(), task.getBookId(), e);
            bookIndexTaskMapper.updateStatusAndProgress(
                    task.getId(), Constants.INDEX_STATUS_FAILED,
                    task.getIndexedPages(), LocalDateTime.now(),
                    e.getMessage() != null && e.getMessage().length() > 900
                            ? e.getMessage().substring(0, 900) : e.getMessage());
        }
    }

    private void indexPdfBook(BookIndexTask task, Book book, String filePath) throws Exception {
        try (PDDocument doc = Loader.loadPDF(Paths.get(filePath).toFile())) {
            int totalPages = doc.getNumberOfPages();
            task.setTotalPages(totalPages);

            bookPageTextMapper.delete(
                    new LambdaQueryWrapper<BookPageText>().eq(BookPageText::getBookId, task.getBookId()));

            List<BookPageText> batch = new ArrayList<>();
            int batchSize = 50;

            for (int i = 1; i <= totalPages; i++) {
                try {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setStartPage(i);
                    stripper.setEndPage(i);
                    String text = stripper.getText(doc);

                    BookPageText pageText = new BookPageText();
                    pageText.setUserId(task.getUserId());
                    pageText.setBookId(task.getBookId());
                    pageText.setBookTitle(book.getTitle());
                    pageText.setPageNum(i);
                    pageText.setPageText(text != null ? text.trim() : "");
                    pageText.setWordCount(text != null ? text.length() : 0);
                    batch.add(pageText);

                    if (batch.size() >= batchSize) {
                        batch.forEach(bookPageTextMapper::insert);
                        batch.clear();
                    }

                    task.setIndexedPages(i);
                    if (i % 10 == 0) {
                        bookIndexTaskMapper.updateById(task);
                    }

                } catch (Exception e) {
                    log.warn("提取第{}页文本失败: {}", i, e.getMessage());
                }
            }

            if (!batch.isEmpty()) {
                batch.forEach(bookPageTextMapper::insert);
            }
        }
    }

    private void indexEpubBook(BookIndexTask task, Book book, String filePath) throws Exception {
        ReaderAdapter adapter = readerAdapterFactory.getAdapter(Constants.FORMAT_EPUB);
        int totalChapters = adapter.getTotalUnits(filePath);
        task.setTotalPages(totalChapters);

        bookPageTextMapper.delete(
                new LambdaQueryWrapper<BookPageText>().eq(BookPageText::getBookId, task.getBookId()));

        for (int i = 0; i < totalChapters; i++) {
            try {
                String text = adapter.getUnitContent(filePath, i);

                BookPageText pageText = new BookPageText();
                pageText.setUserId(task.getUserId());
                pageText.setBookId(task.getBookId());
                pageText.setBookTitle(book.getTitle());
                pageText.setPageNum(i + 1);
                pageText.setPageText(text != null ? text.replaceAll("<[^>]+>", " ").trim() : "");
                pageText.setWordCount(text != null ? text.length() : 0);
                bookPageTextMapper.insert(pageText);

                task.setIndexedPages(i + 1);
                if (i % 10 == 0) {
                    bookIndexTaskMapper.updateById(task);
                }

            } catch (Exception e) {
                log.warn("提取第{}章文本失败: {}", i, e.getMessage());
            }
        }
    }

    public void rebuildIndex(Long bookId) {
        Long userId = TenantContext.getTenantId();
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("书籍不存在");
        }
        if (userId == null) {
            userId = book.getUserId();
        }
        createIndexTask(userId, bookId);
        processIndexTask();
    }

    public void rebuildAllPending() {
        new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                try {
                    processIndexTask();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    public void retryTask(Long taskId) {
        BookIndexTask task = bookIndexTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        task.setStatus(Constants.INDEX_STATUS_PENDING);
        task.setErrorMessage("");
        task.setStartedAt(null);
        task.setFinishedAt(null);
        bookIndexTaskMapper.updateById(task);
        processIndexTask();
    }

    public Page<IndexStatusVO> getIndexStatus(int page, int size, Integer status) {
        LambdaQueryWrapper<BookIndexTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(BookIndexTask::getStatus, status);
        }
        wrapper.orderByDesc(BookIndexTask::getCreatedAt);

        Page<BookIndexTask> taskPage = bookIndexTaskMapper.selectPage(new Page<>(page, size), wrapper);

        Page<IndexStatusVO> voPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        List<IndexStatusVO> vos = taskPage.getRecords().stream().map(this::convertToVO).toList();
        voPage.setRecords(vos);
        return voPage;
    }

    public List<IndexStatusVO> getFailedAlerts() {
        List<BookIndexTask> failedTasks = bookIndexTaskMapper.findByStatus(Constants.INDEX_STATUS_FAILED);
        return failedTasks.stream().map(this::convertToVO).toList();
    }

    public IndexStatusVO getBookIndexStatus(Long bookId) {
        BookIndexTask task = bookIndexTaskMapper.selectOne(
                new LambdaQueryWrapper<BookIndexTask>().eq(BookIndexTask::getBookId, bookId));
        return task != null ? convertToVO(task) : null;
    }

    private IndexStatusVO convertToVO(BookIndexTask task) {
        IndexStatusVO vo = new IndexStatusVO();
        vo.setId(task.getId());
        vo.setUserId(task.getUserId());
        vo.setBookId(task.getBookId());
        vo.setBookTitle(task.getBookTitle());
        vo.setTotalPages(task.getTotalPages());
        vo.setIndexedPages(task.getIndexedPages());
        vo.setStatus(task.getStatus());
        vo.setStatusText(getStatusText(task.getStatus()));
        vo.setErrorMessage(task.getErrorMessage());
        vo.setProgress(task.getTotalPages() > 0
                ? (int) ((task.getIndexedPages() * 100.0) / task.getTotalPages()) : 0);
        vo.setStartedAt(task.getStartedAt());
        vo.setFinishedAt(task.getFinishedAt());
        vo.setCreatedAt(task.getCreatedAt());

        User user = userMapper.selectById(task.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
        }

        return vo;
    }

    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已完成";
            case 3 -> "失败";
            default -> "未知";
        };
    }
}
