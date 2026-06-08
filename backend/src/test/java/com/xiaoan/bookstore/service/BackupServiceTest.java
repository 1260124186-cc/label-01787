package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.dto.BackupTaskVO;
import com.xiaoan.bookstore.dto.ExportDataDTO;
import com.xiaoan.bookstore.dto.StorageStatsVO;
import com.xiaoan.bookstore.entity.Annotation;
import com.xiaoan.bookstore.entity.BackupTask;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.entity.Category;
import com.xiaoan.bookstore.entity.ReadingRecord;
import com.xiaoan.bookstore.entity.User;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.AnnotationMapper;
import com.xiaoan.bookstore.mapper.BackupTaskMapper;
import com.xiaoan.bookstore.mapper.BookMapper;
import com.xiaoan.bookstore.mapper.CategoryMapper;
import com.xiaoan.bookstore.mapper.ReadingRecordMapper;
import com.xiaoan.bookstore.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BackupService 单元测试")
class BackupServiceTest {

    @Mock
    private BackupTaskMapper backupTaskMapper;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private AnnotationMapper annotationMapper;

    @Mock
    private ReadingRecordMapper readingRecordMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private BackupService backupService;

    @TempDir
    Path tempDir;

    private final Long userId = 100L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(backupService, "uploadPath", tempDir.toString());
        ReflectionTestUtils.setField(backupService, "backupPath", tempDir.toString());
        ReflectionTestUtils.setField(backupService, "maxZipSize", 100 * 1024 * 1024L);
    }

    @Nested
    @DisplayName("安全类型转换方法测试")
    class TypeConversionTests {

        @Test
        @DisplayName("getLongValue - null输入应返回null")
        void getLongValue_shouldReturnNullForNull() {
            Long result = invokeGetLongValue(null);
            assertNull(result);
        }

        @Test
        @DisplayName("getLongValue - Integer类型应正确转换")
        void getLongValue_shouldConvertInteger() {
            Long result = invokeGetLongValue(123);
            assertEquals(123L, result);
        }

        @Test
        @DisplayName("getLongValue - Long类型应直接返回")
        void getLongValue_shouldReturnLongDirectly() {
            Long result = invokeGetLongValue(123L);
            assertEquals(123L, result);
        }

        @Test
        @DisplayName("getLongValue - 字符串数字应正确解析")
        void getLongValue_shouldParseStringNumber() {
            Long result = invokeGetLongValue("456");
            assertEquals(456L, result);
        }

        @Test
        @DisplayName("getLongValue - 无效字符串应返回null")
        void getLongValue_shouldReturnNullForInvalidString() {
            Long result = invokeGetLongValue("not_a_number");
            assertNull(result);
        }

        @Test
        @DisplayName("getIntValue - null输入应返回null")
        void getIntValue_shouldReturnNullForNull() {
            Integer result = invokeGetIntValue(null);
            assertNull(result);
        }

        @Test
        @DisplayName("getIntValue - Long类型应正确转换")
        void getIntValue_shouldConvertLong() {
            Integer result = invokeGetIntValue(123L);
            assertEquals(123, result);
        }

        @Test
        @DisplayName("getIntValue - 字符串数字应正确解析")
        void getIntValue_shouldParseStringNumber() {
            Integer result = invokeGetIntValue("456");
            assertEquals(456, result);
        }

        @Test
        @DisplayName("getIntValue - 无效字符串应返回null")
        void getIntValue_shouldReturnNullForInvalidString() {
            Integer result = invokeGetIntValue("not_a_number");
            assertNull(result);
        }

        @Test
        @DisplayName("getStringValue - null输入应返回null")
        void getStringValue_shouldReturnNullForNull() {
            String result = invokeGetStringValue(null);
            assertNull(result);
        }

        @Test
        @DisplayName("getStringValue - 字符串应直接返回")
        void getStringValue_shouldReturnStringDirectly() {
            String result = invokeGetStringValue("test");
            assertEquals("test", result);
        }

        @Test
        @DisplayName("getStringValue - 数字应转换为字符串")
        void getStringValue_shouldConvertNumberToString() {
            String result = invokeGetStringValue(123);
            assertEquals("123", result);
        }

        @Test
        @DisplayName("getStringValue with default - null应返回默认值")
        void getStringValue_shouldReturnDefaultForNull() {
            String result = invokeGetStringValue(null, "default");
            assertEquals("default", result);
        }

        @Test
        @DisplayName("getStringValue with default - 非null应返回原值")
        void getStringValue_shouldReturnValueWhenNotNull() {
            String result = invokeGetStringValue("value", "default");
            assertEquals("value", result);
        }

        private Long invokeGetLongValue(Object obj) {
            try {
                java.lang.reflect.Method method = BackupService.class.getDeclaredMethod("getLongValue", Object.class);
                method.setAccessible(true);
                return (Long) method.invoke(backupService, obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private Integer invokeGetIntValue(Object obj) {
            try {
                java.lang.reflect.Method method = BackupService.class.getDeclaredMethod("getIntValue", Object.class);
                method.setAccessible(true);
                return (Integer) method.invoke(backupService, obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private String invokeGetStringValue(Object obj) {
            try {
                java.lang.reflect.Method method = BackupService.class.getDeclaredMethod("getStringValue", Object.class);
                method.setAccessible(true);
                return (String) method.invoke(backupService, obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private String invokeGetStringValue(Object obj, String defaultValue) {
            try {
                java.lang.reflect.Method method = BackupService.class.getDeclaredMethod("getStringValue", Object.class, String.class);
                method.setAccessible(true);
                return (String) method.invoke(backupService, obj, defaultValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    @DisplayName("文件大小格式化测试")
    class FileSizeFormatTests {

        @Test
        @DisplayName("formatFileSize - null应返回0 B")
        void formatFileSize_shouldReturn0BForNull() {
            String result = invokeFormatFileSize(null);
            assertEquals("0 B", result);
        }

        @Test
        @DisplayName("formatFileSize - 0应返回0 B")
        void formatFileSize_shouldReturn0BForZero() {
            String result = invokeFormatFileSize(0L);
            assertEquals("0 B", result);
        }

        @Test
        @DisplayName("formatFileSize - 字节单位")
        void formatFileSize_shouldUseBytes() {
            String result = invokeFormatFileSize(500L);
            assertEquals("500 B", result);
        }

        @Test
        @DisplayName("formatFileSize - KB单位")
        void formatFileSize_shouldUseKB() {
            String result = invokeFormatFileSize(2 * 1024L);
            assertEquals("2.00 KB", result);
        }

        @Test
        @DisplayName("formatFileSize - MB单位")
        void formatFileSize_shouldUseMB() {
            String result = invokeFormatFileSize(5 * 1024 * 1024L);
            assertEquals("5.00 MB", result);
        }

        @Test
        @DisplayName("formatFileSize - GB单位")
        void formatFileSize_shouldUseGB() {
            String result = invokeFormatFileSize(2L * 1024 * 1024 * 1024);
            assertEquals("2.00 GB", result);
        }

        private String invokeFormatFileSize(Long size) {
            try {
                java.lang.reflect.Method method = BackupService.class.getDeclaredMethod("formatFileSize", Long.class);
                method.setAccessible(true);
                return (String) method.invoke(backupService, size);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    @DisplayName("创建导出任务测试")
    class CreateExportTaskTests {

        @Test
        @DisplayName("应正确创建导出任务")
        void shouldCreateExportTask() {
            when(backupTaskMapper.insert(any(BackupTask.class)))
                    .thenAnswer(invocation -> {
                        BackupTask task = invocation.getArgument(0);
                        task.setId(1L);
                        return 1;
                    });

            BackupTask task = backupService.createExportTask(userId);

            assertNotNull(task);
            assertEquals(userId, task.getUserId());
            assertEquals(Constants.BACKUP_TYPE_EXPORT, task.getTaskType());
            assertEquals(Constants.BACKUP_STATUS_PENDING, task.getStatus());
            assertEquals(0, task.getProgress());
            verify(backupTaskMapper, times(1)).insert(any(BackupTask.class));
        }
    }

    @Nested
    @DisplayName("创建导入任务测试")
    class CreateImportTaskTests {

        @Test
        @DisplayName("应正确创建导入任务")
        void shouldCreateImportTask() throws IOException {
            byte[] content = "test content".getBytes();
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("backup.zip");
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content));

            when(backupTaskMapper.insert(any(BackupTask.class)))
                    .thenAnswer(invocation -> {
                        BackupTask task = invocation.getArgument(0);
                        task.setId(1L);
                        return 1;
                    });

            BackupTask task = backupService.createImportTask(userId, file);

            assertNotNull(task);
            assertEquals(userId, task.getUserId());
            assertEquals(Constants.BACKUP_TYPE_IMPORT, task.getTaskType());
            assertEquals(Constants.BACKUP_STATUS_PENDING, task.getStatus());
            assertTrue(task.getFileName().contains("backup.zip"));
            verify(backupTaskMapper, times(1)).insert(any(BackupTask.class));

            Path backupDir = tempDir.resolve(userId.toString());
            assertTrue(Files.exists(backupDir));
        }

        @Test
        @DisplayName("空文件应抛出异常")
        void shouldRejectEmptyFile() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    backupService.createImportTask(userId, file)
            );

            assertEquals("请选择导入文件", ex.getMessage());
            verify(backupTaskMapper, never()).insert(any());
        }

        @Test
        @DisplayName("非ZIP或JSON文件应抛出异常")
        void shouldRejectNonZipOrJsonFile() throws IOException {
            byte[] content = "test".getBytes();
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("test.txt");

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    backupService.createImportTask(userId, file)
            );

            assertEquals("仅支持ZIP或JSON格式的备份文件", ex.getMessage());
            verify(backupTaskMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("获取任务列表测试")
    class GetTaskListTests {

        @Test
        @DisplayName("应正确获取用户任务列表")
        void shouldGetUserTaskList() {
            List<BackupTask> tasks = Arrays.asList(
                    createBackupTask(1L, userId, Constants.BACKUP_TYPE_EXPORT, Constants.BACKUP_STATUS_COMPLETED),
                    createBackupTask(2L, userId, Constants.BACKUP_TYPE_IMPORT, Constants.BACKUP_STATUS_PROCESSING)
            );
            Page<BackupTask> page = new Page<>(1, 10);
            page.setRecords(tasks);
            page.setTotal(2);

            when(backupTaskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);

            Page<BackupTask> result = backupService.getUserTasks(userId, 1, 10);

            assertNotNull(result);
            assertEquals(2, result.getRecords().size());
            verify(backupTaskMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("应按状态过滤任务")
        void shouldFilterByStatus() {
            List<BackupTask> tasks = Arrays.asList(
                    createBackupTask(1L, userId, Constants.BACKUP_TYPE_EXPORT, Constants.BACKUP_STATUS_COMPLETED)
            );
            Page<BackupTask> page = new Page<>(1, 10);
            page.setRecords(tasks);
            page.setTotal(1);

            when(backupTaskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);

            Page<BackupTask> result = backupService.getUserTasks(userId, 1, 10);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
        }
    }

    @Nested
    @DisplayName("获取任务详情测试")
    class GetTaskDetailTests {

        @Test
        @DisplayName("应正确获取任务详情")
        void shouldGetTaskDetail() {
            BackupTask task = createBackupTask(1L, userId, Constants.BACKUP_TYPE_EXPORT, Constants.BACKUP_STATUS_COMPLETED);
            when(backupTaskMapper.selectById(1L)).thenReturn(task);

            BackupTask result = backupService.getTaskDetail(userId, 1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("任务不存在应抛出异常")
        void shouldThrowExceptionWhenTaskNotFound() {
            when(backupTaskMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    backupService.getTaskDetail(userId, 999L)
            );

            assertEquals("任务不存在", ex.getMessage());
        }

        @Test
        @DisplayName("不属于当前用户的任务应抛出异常")
        void shouldThrowExceptionWhenTaskNotBelongsToOtherUser() {
            BackupTask task = createBackupTask(1L, 200L, Constants.BACKUP_TYPE_EXPORT, Constants.BACKUP_STATUS_COMPLETED);
            when(backupTaskMapper.selectById(1L)).thenReturn(task);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    backupService.getTaskDetail(userId, 1L)
            );

            assertEquals("任务不存在", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("删除任务测试")
    class DeleteTaskTests {

        @Test
        @DisplayName("应正确删除任务")
        void shouldDeleteTask() {
            BackupTask task = createBackupTask(1L, userId, Constants.BACKUP_TYPE_EXPORT, Constants.BACKUP_STATUS_COMPLETED);
            when(backupTaskMapper.selectById(1L)).thenReturn(task);
            when(backupTaskMapper.deleteById(eq(1L))).thenReturn(1);

            backupService.deleteTask(1L);

            verify(backupTaskMapper, times(1)).deleteById(eq(1L));
        }

        @Test
        @DisplayName("任务不存在时不抛出异常")
        void shouldNotThrowExceptionWhenTaskNotFound() {
            when(backupTaskMapper.selectById(999L)).thenReturn(null);

            assertDoesNotThrow(() -> backupService.deleteTask(999L));
            verify(backupTaskMapper, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("管理端任务列表测试")
    class GetAllTasksTests {

        @Test
        @DisplayName("应正确获取所有任务")
        void shouldGetAllTasks() {
            List<BackupTask> tasks = Arrays.asList(
                    createBackupTask(1L, 100L, Constants.BACKUP_TYPE_EXPORT, Constants.BACKUP_STATUS_COMPLETED),
                    createBackupTask(2L, 200L, Constants.BACKUP_TYPE_IMPORT, Constants.BACKUP_STATUS_FAILED)
            );
            Page<BackupTask> page = new Page<>(1, 10);
            page.setRecords(tasks);
            page.setTotal(2);

            when(backupTaskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);

            Page<BackupTask> result = backupService.getAllTasks(null, 1, 10);

            assertNotNull(result);
            assertEquals(2, result.getRecords().size());
        }
    }

    @Nested
    @DisplayName("存储统计测试")
    class GetStorageStatsTests {

        @Test
        @DisplayName("应正确获取存储统计")
        void shouldGetStorageStats() {
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);
            when(bookMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(50L);
            when(backupTaskMapper.getTotalFileSize()).thenReturn(1024L * 1024L * 100);
            when(annotationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(200L);
            when(readingRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(300L);
            when(categoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);

            List<Map<String, Object>> topUsers = new ArrayList<>();
            Map<String, Object> user1 = new HashMap<>();
            user1.put("user_id", 100L);
            user1.put("book_count", 20L);
            user1.put("total_size", 1024L * 1024L * 50);
            topUsers.add(user1);

            when(backupTaskMapper.getTopUsersByStorage()).thenReturn(topUsers);
            when(userMapper.selectById(100L)).thenReturn(createUser(100L, "测试用户"));

            List<Map<String, Object>> dailyData = new ArrayList<>();
            Map<String, Object> day1 = new HashMap<>();
            day1.put("date", "2026-06-01");
            day1.put("total_size", 1024L * 1024L * 10);
            day1.put("book_count", 5L);
            day1.put("user_count", 3L);
            dailyData.add(day1);
            when(backupTaskMapper.getDailyStorageTrend(any(LocalDateTime.class))).thenReturn(dailyData);

            StorageStatsVO vo = backupService.getStorageStats();

            assertNotNull(vo);
            assertEquals(10L, vo.getTotalUsers());
            assertEquals(50L, vo.getTotalBooks());
            assertEquals(1024L * 1024 * 100, vo.getTotalFileSize());
            assertEquals(200L, vo.getTotalAnnotations());
            assertEquals(300L, vo.getTotalReadingRecords());
            assertEquals(10L, vo.getTotalCategories());

            assertNotNull(vo.getTopUsers());
            assertEquals(1, vo.getTopUsers().size());
            assertEquals("测试用户", vo.getTopUsers().get(0).getNickname());

            assertNotNull(vo.getDailyTrend());
            assertEquals(1, vo.getDailyTrend().size());
            assertEquals("2026-06-01", vo.getDailyTrend().get(0).getDate());
        }

        @Test
        @DisplayName("selectCount返回null时应默认为0")
        void shouldDefaultTo0WhenSelectCountReturnsNull() {
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(bookMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(backupTaskMapper.getTotalFileSize()).thenReturn(null);
            when(annotationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(readingRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(categoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(backupTaskMapper.getTopUsersByStorage()).thenReturn(new ArrayList<>());
            when(backupTaskMapper.getDailyStorageTrend(any(LocalDateTime.class))).thenReturn(new ArrayList<>());

            StorageStatsVO vo = backupService.getStorageStats();

            assertNotNull(vo);
            assertEquals(0L, vo.getTotalUsers());
            assertEquals(0L, vo.getTotalBooks());
            assertEquals(0L, vo.getTotalFileSize());
            assertEquals(0L, vo.getTotalAnnotations());
            assertEquals(0L, vo.getTotalReadingRecords());
            assertEquals(0L, vo.getTotalCategories());
        }

        @Test
        @DisplayName("Top用户中userId为null时应跳过")
        void shouldSkipTopUserWhenUserIdIsNull() {
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
            when(bookMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);
            when(backupTaskMapper.getTotalFileSize()).thenReturn(1024L * 1024 * 100);
            when(annotationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(50L);
            when(readingRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(80L);
            when(categoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            List<Map<String, Object>> topUsers = new ArrayList<>();
            Map<String, Object> user1 = new HashMap<>();
            user1.put("user_id", null);
            user1.put("book_count", 20L);
            user1.put("total_size", 1024L * 1024 * 50);
            topUsers.add(user1);

            Map<String, Object> user2 = new HashMap<>();
            user2.put("user_id", 200L);
            user2.put("book_count", 10L);
            user2.put("total_size", 1024L * 1024 * 30);
            topUsers.add(user2);

            when(backupTaskMapper.getTopUsersByStorage()).thenReturn(topUsers);
            when(userMapper.selectById(200L)).thenReturn(createUser(200L, "用户2"));
            when(backupTaskMapper.getDailyStorageTrend(any(LocalDateTime.class))).thenReturn(new ArrayList<>());

            StorageStatsVO vo = backupService.getStorageStats();

            assertEquals(1, vo.getTopUsers().size());
            assertEquals(200L, vo.getTopUsers().get(0).getUserId());
            assertEquals("用户2", vo.getTopUsers().get(0).getNickname());
        }
    }

    @Nested
    @DisplayName("管理端删除任务测试")
    class AdminDeleteTaskTests {

        @Test
        @DisplayName("管理员应能删除任意任务")
        void adminShouldDeleteAnyTask() {
            BackupTask task = createBackupTask(1L, 200L, Constants.BACKUP_TYPE_EXPORT, Constants.BACKUP_STATUS_COMPLETED);
            when(backupTaskMapper.selectById(1L)).thenReturn(task);
            when(backupTaskMapper.deleteById(eq(1L))).thenReturn(1);

            backupService.deleteTask(1L);

            verify(backupTaskMapper, times(1)).deleteById(eq(1L));
        }

        @Test
        @DisplayName("任务不存在时不抛出异常")
        void shouldNotThrowExceptionWhenTaskNotFound() {
            when(backupTaskMapper.selectById(999L)).thenReturn(null);

            assertDoesNotThrow(() -> backupService.deleteTask(999L));
        }
    }

    @Nested
    @DisplayName("导入结果测试")
    class GetImportResultTests {

        @Test
        @DisplayName("非导入任务应抛出异常")
        void shouldThrowExceptionWhenNotImportTask() {
            BackupTask task = createBackupTask(1L, userId, Constants.BACKUP_TYPE_EXPORT, Constants.BACKUP_STATUS_COMPLETED);
            when(backupTaskMapper.selectById(1L)).thenReturn(task);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    backupService.getImportResult(userId, 1L)
            );

            assertEquals("不是导入任务", ex.getMessage());
        }
    }

    private BackupTask createBackupTask(Long id, Long userId, Integer taskType, Integer status) {
        BackupTask task = new BackupTask();
        task.setId(id);
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setStatus(status);
        task.setProgress(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    private User createUser(Long id, String nickname) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        return user;
    }
}
