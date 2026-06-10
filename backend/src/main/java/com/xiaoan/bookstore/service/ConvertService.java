package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.entity.ConvertTask;
import com.xiaoan.bookstore.entity.QuotaVO;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.ConvertTaskMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class ConvertService {

    private static final Logger log = LoggerFactory.getLogger(ConvertService.class);
    private final ConvertTaskMapper convertTaskMapper;
    private final BookService bookService;
    private final MembershipService membershipService;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Value("${app.convert.threads:2}")
    private int convertThreads;

    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private static final int PRIORITY_VIP = 10;
    private static final int PRIORITY_FREE = 1;

    @PostConstruct
    public void init() {
        executorService = new ThreadPoolExecutor(
                convertThreads,
                convertThreads,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r, "convert-worker");
                    t.setDaemon(true);
                    return t;
                }
        );
        running.set(true);
        for (int i = 0; i < convertThreads; i++) {
            executorService.submit(this::convertWorker);
        }
        log.info("转图队列服务已启动，线程数: {}", convertThreads);
    }

    public ConvertTask createTask(Long userId, Long bookId) {
        Book book = bookService.detail(userId, bookId);

        int pending = convertTaskMapper.countPendingByUserId(userId);
        if (pending >= 3) {
            throw new BusinessException("您有太多待处理的转图任务，请稍后再试");
        }

        QuotaVO quota = membershipService.getQuota(userId);
        int priority = quota.getIsVip() ? PRIORITY_VIP : PRIORITY_FREE;

        ConvertTask task = new ConvertTask();
        task.setUserId(userId);
        task.setBookId(bookId);
        task.setBookTitle(book.getTitle());
        task.setTotalPages(book.getPageCount());
        task.setConvertedPages(0);
        task.setPriority(priority);
        task.setStatus(0);
        convertTaskMapper.insert(task);

        log.info("创建转图任务: taskId={}, userId={}, bookId={}, priority={}",
                task.getId(), userId, bookId, priority);
        return task;
    }

    public ConvertTask getTask(Long userId, Long taskId) {
        ConvertTask task = convertTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        TenantValidator.validateCrossTenant(task.getUserId(), TenantContext.getTenantId());
        return task;
    }

    public ConvertTask getLatestTask(Long userId, Long bookId) {
        LambdaQueryWrapper<ConvertTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConvertTask::getUserId, userId)
                .eq(ConvertTask::getBookId, bookId)
                .orderByDesc(ConvertTask::getCreatedAt)
                .last("LIMIT 1");
        return convertTaskMapper.selectOne(wrapper);
    }

    public Page<ConvertTask> myTasks(Long userId, int page, int size) {
        LambdaQueryWrapper<ConvertTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConvertTask::getUserId, userId)
                .orderByDesc(ConvertTask::getCreatedAt);
        return convertTaskMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private void convertWorker() {
        while (running.get()) {
            try {
                ConvertTask task = pickNextTask();
                if (task == null) {
                    Thread.sleep(2000);
                    continue;
                }
                processTask(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("转图工作线程异常", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private ConvertTask pickNextTask() {
        return convertTaskMapper.selectOne(
                new LambdaQueryWrapper<ConvertTask>()
                        .eq(ConvertTask::getStatus, 0)
                        .orderByDesc(ConvertTask::getPriority)
                        .orderByAsc(ConvertTask::getCreatedAt)
                        .last("LIMIT 1")
        );
    }

    private void processTask(ConvertTask task) {
        task.setStatus(1);
        task.setStartedAt(LocalDateTime.now());
        convertTaskMapper.updateById(task);

        log.info("开始处理转图任务: taskId={}, book={}", task.getId(), task.getBookTitle());

        try {
            Book book = bookService.detail(task.getUserId(), task.getBookId());
            Path pdfPath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();

            String imagesDir = task.getBookId() + "_images";
            Path outputDir = Paths.get(uploadPath, String.valueOf(task.getUserId()), imagesDir);
            Files.createDirectories(outputDir);

            try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
                PDFRenderer renderer = new PDFRenderer(doc);
                int total = doc.getNumberOfPages();
                task.setTotalPages(total);

                for (int i = 0; i < total; i++) {
                    BufferedImage image = renderer.renderImageWithDPI(i, 150);
                    File outputFile = outputDir.resolve("page_" + (i + 1) + ".png").toFile();
                    ImageIO.write(image, "png", outputFile);

                    task.setConvertedPages(i + 1);
                    convertTaskMapper.updateById(task);

                    if (!running.get()) {
                        break;
                    }
                }
            }

            task.setStatus(2);
            task.setFinishedAt(LocalDateTime.now());
            convertTaskMapper.updateById(task);

            log.info("转图任务完成: taskId={}, totalPages={}", task.getId(), task.getTotalPages());
        } catch (Exception e) {
            log.error("转图任务失败: taskId={}", task.getId(), e);
            task.setStatus(0);
            task.setErrorMsg(e.getMessage());
            convertTaskMapper.updateById(task);
        }
    }

    public Page<ConvertTask> adminList(int page, int size, Integer status) {
        LambdaQueryWrapper<ConvertTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ConvertTask::getStatus, status);
        }
        wrapper.orderByDesc(ConvertTask::getCreatedAt);
        return convertTaskMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public ConvertTask getTaskById(Long id) {
        return convertTaskMapper.selectById(id);
    }

    public int getQueuePosition(Long taskId) {
        ConvertTask task = convertTaskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 0) {
            return 0;
        }
        Long count = convertTaskMapper.selectCount(
                new LambdaQueryWrapper<ConvertTask>()
                        .eq(ConvertTask::getStatus, 0)
                        .and(w -> w.gt(ConvertTask::getPriority, task.getPriority())
                                .or()
                                .and(w2 -> w2.eq(ConvertTask::getPriority, task.getPriority())
                                        .lt(ConvertTask::getCreatedAt, task.getCreatedAt())))
        );
        return count.intValue() + 1;
    }
}
