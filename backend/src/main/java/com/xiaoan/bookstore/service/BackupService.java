package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.CSVWriter;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.dto.*;
import com.xiaoan.bookstore.entity.*;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final BackupTaskMapper backupTaskMapper;
    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final AnnotationMapper annotationMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final UserMapper userMapper;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Value("${app.backup.path}")
    private String backupPath;

    @Value("${app.backup.max-zip-size}")
    private long maxZipSize;

    private final ObjectMapper objectMapper = createObjectMapper();

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    public BackupTask createExportTask(Long userId) {
        BackupTask task = new BackupTask();
        task.setUserId(userId);
        task.setTaskType(Constants.BACKUP_TYPE_EXPORT);
        task.setStatus(Constants.BACKUP_STATUS_PENDING);
        task.setProgress(0);
        backupTaskMapper.insert(task);
        log.info("创建导出任务: userId={}, taskId={}", userId, task.getId());
        return task;
    }

    @Async
    public void processExportAsync(Long taskId) {
        BackupTask task = backupTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("导出任务不存在: taskId={}", taskId);
            return;
        }
        try {
            task.setStatus(Constants.BACKUP_STATUS_PROCESSING);
            task.setProgress(0);
            backupTaskMapper.updateById(task);
            processExport(task);
        } catch (Exception e) {
            log.error("导出任务处理失败", e);
            task.setStatus(Constants.BACKUP_STATUS_FAILED);
            task.setErrorMessage(e.getMessage());
            backupTaskMapper.updateById(task);
        }
    }

    private void processExport(BackupTask task) throws Exception {
        Long userId = task.getUserId();
        Path userBackupDir = Paths.get(backupPath, userId.toString()).toAbsolutePath().normalize();
        Files.createDirectories(userBackupDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "backup_" + userId + "_" + timestamp + ".zip";
        Path zipPath = userBackupDir.resolve(fileName);

        task.setProgress(5);
        backupTaskMapper.updateById(task);

        LambdaQueryWrapper<Category> catWrapper = new LambdaQueryWrapper<>();
        catWrapper.eq(Category::getUserId, userId);
        catWrapper.orderByAsc(Category::getSortOrder);
        List<Category> categories = categoryMapper.selectList(catWrapper);

        task.setProgress(10);
        backupTaskMapper.updateById(task);

        LambdaQueryWrapper<Book> bookWrapper = new LambdaQueryWrapper<>();
        bookWrapper.eq(Book::getUserId, userId);
        bookWrapper.eq(Book::getStatus, Constants.STATUS_ENABLED);
        List<Book> books = bookMapper.selectList(bookWrapper);

        task.setProgress(15);
        backupTaskMapper.updateById(task);

        List<Long> bookIds = books.stream().map(Book::getId).collect(Collectors.toList());

        List<Annotation> annotations = Collections.emptyList();
        if (!bookIds.isEmpty()) {
            LambdaQueryWrapper<Annotation> annWrapper = new LambdaQueryWrapper<>();
            annWrapper.eq(Annotation::getUserId, userId);
            annWrapper.in(Annotation::getBookId, bookIds);
            annotations = annotationMapper.selectList(annWrapper);
        }

        task.setProgress(20);
        backupTaskMapper.updateById(task);

        List<ReadingRecord> records = Collections.emptyList();
        if (!bookIds.isEmpty()) {
            LambdaQueryWrapper<ReadingRecord> recWrapper = new LambdaQueryWrapper<>();
            recWrapper.eq(ReadingRecord::getUserId, userId);
            recWrapper.in(ReadingRecord::getBookId, bookIds);
            records = readingRecordMapper.selectList(recWrapper);
        }

        task.setProgress(25);
        backupTaskMapper.updateById(task);

        Map<Long, String> bookTitleMap = books.stream()
                .collect(Collectors.toMap(Book::getId, Book::getTitle));

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            BackupManifest manifest = new BackupManifest();
            manifest.setVersion("1.0.0");
            manifest.setExportedAt(LocalDateTime.now());
            manifest.setSource("xiaoan-bookstore");
            manifest.setSchemaVersion(Constants.BACKUP_SCHEMA_VERSION);

            BackupManifest.BackupStats stats = new BackupManifest.BackupStats();
            stats.setBookCount(books.size());
            stats.setAnnotationCount(annotations.size());
            stats.setRecordCount(records.size());
            stats.setCategoryCount(categories.size());
            long totalFileSize = books.stream().mapToLong(Book::getFileSize).sum();
            stats.setTotalFileSize(totalFileSize);
            manifest.setStats(stats);

            addToZip(zos, "manifest.json", objectMapper.writeValueAsBytes(manifest));
            task.setProgress(30);
            backupTaskMapper.updateById(task);

            List<ExportDataDTO.CategoryExport> categoryExports = categories.stream().map(cat -> {
                ExportDataDTO.CategoryExport ce = new ExportDataDTO.CategoryExport();
                ce.setId(cat.getId());
                ce.setName(cat.getName());
                ce.setSortOrder(cat.getSortOrder());
                ce.setCreatedAt(cat.getCreatedAt());
                return ce;
            }).collect(Collectors.toList());
            addToZip(zos, "categories.json", objectMapper.writeValueAsBytes(categoryExports));

            task.setProgress(35);
            backupTaskMapper.updateById(task);

            List<ExportDataDTO.BookExport> bookExports = books.stream().map(book -> {
                ExportDataDTO.BookExport be = new ExportDataDTO.BookExport();
                be.setId(book.getId());
                be.setTitle(book.getTitle());
                be.setAuthor(book.getAuthor());
                be.setOriginalFileName(book.getTitle() + ".pdf");
                be.setFileSize(book.getFileSize());
                be.setPageCount(book.getPageCount());
                be.setCategoryId(book.getCategoryId());
                be.setLastPage(book.getLastPage());
                be.setCreatedAt(book.getCreatedAt());
                be.setUpdatedAt(book.getUpdatedAt());
                return be;
            }).collect(Collectors.toList());
            addToZip(zos, "books.json", objectMapper.writeValueAsBytes(bookExports));

            task.setProgress(40);
            backupTaskMapper.updateById(task);

            List<ExportDataDTO.AnnotationExport> annExports = annotations.stream().map(ann -> {
                ExportDataDTO.AnnotationExport ae = new ExportDataDTO.AnnotationExport();
                ae.setId(ann.getId());
                ae.setBookId(ann.getBookId());
                ae.setBookTitle(bookTitleMap.getOrDefault(ann.getBookId(), "未知书籍"));
                ae.setPageNum(ann.getPageNum());
                ae.setSelectedText(ann.getSelectedText());
                ae.setContent(ann.getContent());
                ae.setType(ann.getType());
                ae.setCreatedAt(ann.getCreatedAt());
                ae.setUpdatedAt(ann.getUpdatedAt());
                return ae;
            }).collect(Collectors.toList());
            addToZip(zos, "annotations.json", objectMapper.writeValueAsBytes(annExports));

            task.setProgress(50);
            backupTaskMapper.updateById(task);

            ByteArrayOutputStream csvBaos = new ByteArrayOutputStream();
            try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(csvBaos, StandardCharsets.UTF_8))) {
                String[] header = {"ID", "书籍ID", "书籍名称", "开始时间", "结束时间", "阅读时长(秒)", "阅读页码", "创建时间"};
                writer.writeNext(header);

                for (ReadingRecord record : records) {
                    String[] row = {
                            String.valueOf(record.getId()),
                            String.valueOf(record.getBookId()),
                            bookTitleMap.getOrDefault(record.getBookId(), "未知书籍"),
                            record.getStartTime() != null ? record.getStartTime().toString() : "",
                            record.getEndTime() != null ? record.getEndTime().toString() : "",
                            String.valueOf(record.getDuration()),
                            String.valueOf(record.getLastPage()),
                            record.getCreatedAt() != null ? record.getCreatedAt().toString() : ""
                    };
                    writer.writeNext(row);
                }
            }
            addToZip(zos, "reading_records.csv", csvBaos.toByteArray());

            task.setProgress(60);
            backupTaskMapper.updateById(task);

            int totalBooks = books.size();
            for (int i = 0; i < books.size(); i++) {
                Book book = books.get(i);
                Path pdfPath = Paths.get(uploadPath, book.getFilePath()).toAbsolutePath().normalize();
                if (Files.exists(pdfPath)) {
                    String entryName = "pdfs/" + book.getId() + "_" + sanitizeFileName(book.getTitle()) + ".pdf";
                    addFileToZip(zos, entryName, pdfPath);
                }
                int progress = 60 + (int) (((i + 1) / (double) totalBooks) * 35);
                if (progress % 5 == 0) {
                    task.setProgress(progress);
                    backupTaskMapper.updateById(task);
                }
            }

            task.setProgress(95);
            backupTaskMapper.updateById(task);
        }

        long fileSize = Files.size(zipPath);
        task.setStatus(Constants.BACKUP_STATUS_COMPLETED);
        task.setFileName(fileName);
        task.setFilePath(userId + "/" + fileName);
        task.setFileSize(fileSize);
        task.setBookCount(books.size());
        task.setAnnotationCount(annotations.size());
        task.setRecordCount(records.size());
        task.setCategoryCount(categories.size());
        task.setProgress(100);
        task.setExpiredAt(LocalDateTime.now().plusDays(Constants.BACKUP_EXPIRE_DAYS));
        backupTaskMapper.updateById(task);

        log.info("导出任务完成: taskId={}, fileName={}, size={}", task.getId(), fileName, fileSize);
    }

    private void addToZip(ZipOutputStream zos, String entryName, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private void addFileToZip(ZipOutputStream zos, String entryName, Path filePath) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        Files.copy(filePath, zos);
        zos.closeEntry();
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5.-]", "_");
    }

    public Page<BackupTask> getUserTasks(Long userId, int page, int size) {
        LambdaQueryWrapper<BackupTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BackupTask::getUserId, userId);
        wrapper.orderByDesc(BackupTask::getCreatedAt);
        return backupTaskMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public BackupTask getTaskDetail(Long userId, Long taskId) {
        BackupTask task = backupTaskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new BusinessException("任务不存在");
        }
        return task;
    }

    public Path getBackupFilePath(BackupTask task) {
        return Paths.get(backupPath, task.getFilePath()).toAbsolutePath().normalize();
    }

    @Transactional
    public BackupTask createImportTask(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("请选择导入文件");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || (!originalName.toLowerCase().endsWith(".zip") && !originalName.toLowerCase().endsWith(".json"))) {
            throw new BusinessException("仅支持ZIP或JSON格式的备份文件");
        }

        try {
            Path userBackupDir = Paths.get(backupPath, userId.toString(), "imports").toAbsolutePath().normalize();
            Files.createDirectories(userBackupDir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "import_" + userId + "_" + timestamp + "_" + sanitizeFileName(originalName);
            Path filePath = userBackupDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            BackupTask task = new BackupTask();
            task.setUserId(userId);
            task.setTaskType(Constants.BACKUP_TYPE_IMPORT);
            task.setStatus(Constants.BACKUP_STATUS_PENDING);
            task.setFileName(fileName);
            task.setFilePath(userId + "/imports/" + fileName);
            task.setFileSize(Files.size(filePath));
            task.setProgress(0);
            backupTaskMapper.insert(task);

            log.info("创建导入任务: userId={}, taskId={}, fileName={}", userId, task.getId(), fileName);
            return task;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建导入任务失败", e);
            throw new BusinessException("导入文件处理失败: " + e.getMessage());
        }
    }

    @Async
    public void processImportAsync(Long taskId) {
        BackupTask task = backupTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("导入任务不存在: taskId={}", taskId);
            return;
        }
        try {
            task.setStatus(Constants.BACKUP_STATUS_PROCESSING);
            task.setProgress(0);
            backupTaskMapper.updateById(task);
            processImport(task);
        } catch (Exception e) {
            log.error("导入任务处理失败", e);
            task.setStatus(Constants.BACKUP_STATUS_FAILED);
            task.setErrorMessage(e.getMessage());
            backupTaskMapper.updateById(task);
        }
    }

    @Transactional
    protected ExportDataDTO.ImportResult processImport(BackupTask task) throws Exception {
        Long userId = task.getUserId();
        Path importPath = getBackupFilePath(task);

        if (!Files.exists(importPath)) {
            throw new BusinessException("导入文件不存在");
        }

        task.setProgress(10);
        backupTaskMapper.updateById(task);

        byte[] jsonData;
        String fileName = importPath.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".zip")) {
            jsonData = extractJsonFromZip(importPath);
        } else {
            jsonData = Files.readAllBytes(importPath);
        }

        task.setProgress(20);
        backupTaskMapper.updateById(task);

        Map<String, Object> importData = objectMapper.readValue(jsonData, new TypeReference<Map<String, Object>>() {});

        if (!importData.containsKey("annotations") && !importData.containsKey("categories")) {
            throw new BusinessException("导入文件格式不正确，缺少必要的数据");
        }

        task.setProgress(30);
        backupTaskMapper.updateById(task);

        ExportDataDTO.ImportResult result = new ExportDataDTO.ImportResult();
        result.setBooksToLink(new ArrayList<>());
        result.setWarnings(new ArrayList<>());

        LambdaQueryWrapper<Category> existingCatWrapper = new LambdaQueryWrapper<>();
        existingCatWrapper.eq(Category::getUserId, userId);
        List<Category> existingCategories = categoryMapper.selectList(existingCatWrapper);
        Set<String> existingCategoryNames = existingCategories.stream()
                .map(Category::getName)
                .collect(Collectors.toSet());

        Map<Long, Long> oldToNewCategoryId = new HashMap<>();

        if (importData.containsKey("categories")) {
            List<Map<String, Object>> categories = (List<Map<String, Object>>) importData.get("categories");
            int catImported = 0;
            int catSkipped = 0;

            for (int i = 0; i < categories.size(); i++) {
                Map<String, Object> cat = categories.get(i);
                String name = (String) cat.get("name");
                if (name == null || name.isBlank()) continue;

                if (existingCategoryNames.contains(name)) {
                    catSkipped++;
                    Long oldId = cat.get("id") != null ? Long.valueOf(cat.get("id").toString()) : null;
                    if (oldId != null) {
                        for (Category existing : existingCategories) {
                            if (existing.getName().equals(name)) {
                                oldToNewCategoryId.put(oldId, existing.getId());
                                break;
                            }
                        }
                    }
                    continue;
                }

                Category newCat = new Category();
                newCat.setUserId(userId);
                newCat.setName(name);
                Integer sortOrder = cat.get("sortOrder") != null ? Integer.valueOf(cat.get("sortOrder").toString()) : i;
                newCat.setSortOrder(sortOrder);
                categoryMapper.insert(newCat);

                Long existingOldId = cat.get("id") != null ? Long.valueOf(cat.get("id").toString()) : null;
                if (existingOldId != null) {
                    oldToNewCategoryId.put(existingOldId, newCat.getId());
                }
                catImported++;
            }

            result.setCategoryImported(catImported);
            result.setCategorySkipped(catSkipped);
        }

        task.setProgress(50);
        backupTaskMapper.updateById(task);

        Map<Long, String> bookInfoMap = new HashMap<>();
        if (importData.containsKey("books")) {
            List<Map<String, Object>> books = (List<Map<String, Object>>) importData.get("books");
            for (Map<String, Object> book : books) {
                Long oldId = book.get("id") != null ? Long.valueOf(book.get("id").toString()) : null;
                String title = (String) book.get("title");
                String author = book.get("author") != null ? (String) book.get("author") : "";
                if (oldId != null) {
                    bookInfoMap.put(oldId, title + "||" + author);
                }

                ExportDataDTO.BookImportItem item = new ExportDataDTO.BookImportItem();
                item.setOriginalBookId(String.valueOf(oldId));
                item.setTitle(title);
                item.setAuthor(author);
                item.setAnnotationCount(0);
                item.setRecordCount(0);
                result.getBooksToLink().add(item);
            }
        }

        if (importData.containsKey("annotations")) {
            List<Map<String, Object>> annotations = (List<Map<String, Object>>) importData.get("annotations");
            int annImported = 0;
            int annSkipped = 0;

            LambdaQueryWrapper<Book> userBookWrapper = new LambdaQueryWrapper<>();
            userBookWrapper.eq(Book::getUserId, userId);
            userBookWrapper.eq(Book::getStatus, Constants.STATUS_ENABLED);
            List<Book> userBooks = bookMapper.selectList(userBookWrapper);

            Map<String, Long> titleToBookId = new HashMap<>();
            for (Book book : userBooks) {
                titleToBookId.put(book.getTitle().toLowerCase(), book.getId());
            }

            for (Map<String, Object> ann : annotations) {
                Long oldBookId = ann.get("bookId") != null ? Long.valueOf(ann.get("bookId").toString()) : null;
                String bookTitle = ann.get("bookTitle") != null ? (String) ann.get("bookTitle") : "";

                Long newBookId = null;
                if (oldBookId != null && bookInfoMap.containsKey(oldBookId)) {
                    String[] info = bookInfoMap.get(oldBookId).split("\\|\\|");
                    String title = info[0];
                    newBookId = titleToBookId.get(title.toLowerCase());
                }

                if (newBookId == null && !bookTitle.isBlank()) {
                    newBookId = titleToBookId.get(bookTitle.toLowerCase());
                }

                if (newBookId == null) {
                    annSkipped++;
                    continue;
                }

                Annotation newAnn = new Annotation();
                newAnn.setUserId(userId);
                newAnn.setBookId(newBookId);
                newAnn.setPageNum(ann.get("pageNum") != null ? Integer.valueOf(ann.get("pageNum").toString()) : 1);
                newAnn.setSelectedText(ann.get("selectedText") != null ? (String) ann.get("selectedText") : "");
                newAnn.setContent(ann.get("content") != null ? (String) ann.get("content") : "");
                newAnn.setType(ann.get("type") != null ? Integer.valueOf(ann.get("type").toString()) : Constants.ANNOTATION_NOTE);
                annotationMapper.insert(newAnn);
                annImported++;

                for (ExportDataDTO.BookImportItem item : result.getBooksToLink()) {
                    if (item.getOriginalBookId().equals(String.valueOf(oldBookId))) {
                        item.setAnnotationCount(item.getAnnotationCount() + 1);
                        break;
                    }
                }
            }

            result.setAnnotationImported(annImported);
            result.setAnnotationSkipped(annSkipped);
        }

        task.setProgress(75);
        backupTaskMapper.updateById(task);

        if (importData.containsKey("reading_records")) {
            List<Map<String, Object>> records = (List<Map<String, Object>>) importData.get("reading_records");
            int recImported = 0;
            int recSkipped = 0;

            LambdaQueryWrapper<Book> userBookWrapper = new LambdaQueryWrapper<>();
            userBookWrapper.eq(Book::getUserId, userId);
            userBookWrapper.eq(Book::getStatus, Constants.STATUS_ENABLED);
            List<Book> userBooks = bookMapper.selectList(userBookWrapper);

            Map<String, Long> titleToBookId = new HashMap<>();
            for (Book book : userBooks) {
                titleToBookId.put(book.getTitle().toLowerCase(), book.getId());
            }

            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

            for (Map<String, Object> rec : records) {
                Long oldBookId = rec.get("bookId") != null ? Long.valueOf(rec.get("bookId").toString()) : null;
                String bookTitle = rec.get("bookTitle") != null ? (String) rec.get("bookTitle") : "";

                Long newBookId = null;
                if (oldBookId != null && bookInfoMap.containsKey(oldBookId)) {
                    String[] info = bookInfoMap.get(oldBookId).split("\\|\\|");
                    String title = info[0];
                    newBookId = titleToBookId.get(title.toLowerCase());
                }

                if (newBookId == null && !bookTitle.isBlank()) {
                    newBookId = titleToBookId.get(bookTitle.toLowerCase());
                }

                if (newBookId == null) {
                    recSkipped++;
                    continue;
                }

                ReadingRecord newRec = new ReadingRecord();
                newRec.setUserId(userId);
                newRec.setBookId(newBookId);
                String startTimeStr = rec.get("startTime") != null ? (String) rec.get("startTime") : null;
                if (startTimeStr != null) {
                    try {
                        newRec.setStartTime(LocalDateTime.parse(startTimeStr.replace(" ", "T"), formatter));
                    } catch (Exception e) {
                        newRec.setStartTime(LocalDateTime.now());
                    }
                } else {
                    newRec.setStartTime(LocalDateTime.now());
                }

                String endTimeStr = rec.get("endTime") != null ? (String) rec.get("endTime") : null;
                if (endTimeStr != null) {
                    try {
                        newRec.setEndTime(LocalDateTime.parse(endTimeStr.replace(" ", "T"), formatter));
                    } catch (Exception ignored) {}
                }
                newRec.setDuration(rec.get("duration") != null ? Integer.valueOf(rec.get("duration").toString()) : 0);
                newRec.setLastPage(rec.get("lastPage") != null ? Integer.valueOf(rec.get("lastPage").toString()) : 0);
                readingRecordMapper.insert(newRec);
                recImported++;

                for (ExportDataDTO.BookImportItem item : result.getBooksToLink()) {
                    if (item.getOriginalBookId().equals(String.valueOf(oldBookId))) {
                        item.setRecordCount(item.getRecordCount() + 1);
                        break;
                    }
                }
            }

            result.setRecordImported(recImported);
            result.setRecordSkipped(recSkipped);
        }

        task.setProgress(90);
        backupTaskMapper.updateById(task);

        List<ExportDataDTO.BookImportItem> booksToLink = result.getBooksToLink().stream()
                .filter(item -> item.getAnnotationCount() > 0 || item.getRecordCount() > 0)
                .collect(Collectors.toList());
        result.setBooksToLink(booksToLink);

        if (!booksToLink.isEmpty()) {
            result.getWarnings().add("部分数据已导入但未关联到书籍，请在导入结果中手动关联PDF文件");
        }

        task.setStatus(Constants.BACKUP_STATUS_COMPLETED);
        task.setCategoryCount(result.getCategoryImported());
        task.setAnnotationCount(result.getAnnotationImported());
        task.setRecordCount(result.getRecordImported());
        task.setBookCount((int) booksToLink.stream()
                .filter(item -> item.getAnnotationCount() > 0 || item.getRecordCount() > 0)
                .count());
        task.setProgress(100);
        backupTaskMapper.updateById(task);

        log.info("导入任务完成: taskId={}, 分类:{}/{}, 批注:{}/{}, 记录:{}/{}",
                task.getId(),
                result.getCategoryImported(), result.getCategoryImported() + result.getCategorySkipped(),
                result.getAnnotationImported(), result.getAnnotationImported() + result.getAnnotationSkipped(),
                result.getRecordImported(), result.getRecordImported() + result.getRecordSkipped());

        return result;
    }

    private byte[] extractJsonFromZip(Path zipPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();
                if (name.endsWith(".json") && !name.contains("/")) {
                    if (name.contains("annotations") || name.contains("data") || name.equals("manifest.json")) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            baos.write(buffer, 0, len);
                        }
                        return baos.toByteArray();
                    }
                }
            }
        }
        throw new BusinessException("ZIP文件中未找到有效的数据文件");
    }

    public ExportDataDTO.ImportResult getImportResult(Long userId, Long taskId) {
        BackupTask task = getTaskDetail(userId, taskId);
        if (task.getTaskType() != Constants.BACKUP_TYPE_IMPORT) {
            throw new BusinessException("不是导入任务");
        }
        try {
            return processImport(task);
        } catch (Exception e) {
            throw new BusinessException("获取导入结果失败: " + e.getMessage());
        }
    }

    public Page<BackupTask> getAllTasks(Integer status, int page, int size) {
        LambdaQueryWrapper<BackupTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(BackupTask::getStatus, status);
        }
        wrapper.orderByDesc(BackupTask::getCreatedAt);
        return backupTaskMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public StorageStatsVO getStorageStats() {
        StorageStatsVO vo = new StorageStatsVO();

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getStatus, Constants.STATUS_ENABLED);
        vo.setTotalUsers(userMapper.selectCount(userWrapper));

        LambdaQueryWrapper<Book> bookWrapper = new LambdaQueryWrapper<>();
        bookWrapper.eq(Book::getStatus, Constants.STATUS_ENABLED);
        vo.setTotalBooks(bookMapper.selectCount(bookWrapper));

        vo.setTotalFileSize(backupTaskMapper.getTotalFileSize());
        vo.setTotalFileSizeText(formatFileSize(vo.getTotalFileSize()));

        LambdaQueryWrapper<Annotation> annWrapper = new LambdaQueryWrapper<>();
        vo.setTotalAnnotations(annotationMapper.selectCount(annWrapper));

        LambdaQueryWrapper<ReadingRecord> recWrapper = new LambdaQueryWrapper<>();
        vo.setTotalReadingRecords(readingRecordMapper.selectCount(recWrapper));

        LambdaQueryWrapper<Category> catWrapper = new LambdaQueryWrapper<>();
        vo.setTotalCategories(categoryMapper.selectCount(catWrapper));

        List<Map<String, Object>> topUsersData = backupTaskMapper.getTopUsersByStorage();
        List<StorageStatsVO.UserStorageStats> topUsers = new ArrayList<>();
        for (Map<String, Object> row : topUsersData) {
            StorageStatsVO.UserStorageStats stats = new StorageStatsVO.UserStorageStats();
            Long userId = ((Number) row.get("user_id")).longValue();
            User user = userMapper.selectById(userId);
            stats.setUserId(userId);
            stats.setNickname(user != null ? user.getNickname() : "未知用户");
            stats.setBookCount(((Number) row.get("book_count")).longValue());
            Long fileSize = ((Number) row.get("total_size")).longValue();
            stats.setFileSize(fileSize);
            stats.setFileSizeText(formatFileSize(fileSize));
            stats.setPercentage(vo.getTotalFileSize() > 0 ? (fileSize * 100.0 / vo.getTotalFileSize()) : 0);
            topUsers.add(stats);
        }
        vo.setTopUsers(topUsers);

        LocalDateTime startDate = LocalDate.now().minusDays(30).atStartOfDay();
        List<Map<String, Object>> dailyData = backupTaskMapper.getDailyStorageTrend(startDate);
        List<StorageStatsVO.DailyStorageStats> dailyTrend = new ArrayList<>();
        for (Map<String, Object> row : dailyData) {
            StorageStatsVO.DailyStorageStats stats = new StorageStatsVO.DailyStorageStats();
            stats.setDate(row.get("date").toString());
            stats.setFileSize(((Number) row.get("total_size")).longValue());
            stats.setBookCount(((Number) row.get("book_count")).longValue());
            stats.setUserCount(((Number) row.get("user_count")).longValue());
            dailyTrend.add(stats);
        }
        vo.setDailyTrend(dailyTrend);

        return vo;
    }

    public void deleteTask(Long taskId) {
        BackupTask task = backupTaskMapper.selectById(taskId);
        if (task != null) {
            try {
                Path filePath = getBackupFilePath(task);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            } catch (Exception e) {
                log.warn("删除备份文件失败: {}", e.getMessage());
            }
            backupTaskMapper.deleteById(taskId);
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
