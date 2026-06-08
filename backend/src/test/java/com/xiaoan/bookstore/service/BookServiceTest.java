package com.xiaoan.bookstore.service;

import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.entity.ContentAudit;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.BookMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService 单元测试 - 上传功能")
class BookServiceTest {

    @Mock
    private BookMapper bookMapper;

    @Mock
    private ContentComplianceService contentComplianceService;

    @InjectMocks
    private BookService bookService;

    @TempDir
    Path tempDir;

    private final Long userId = 100L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookService, "uploadPath", tempDir.toString());
        ReflectionTestUtils.setField(bookService, "maxFileSize", 150 * 1024 * 1024L);
    }

    @Nested
    @DisplayName("上传校验 - 参数校验")
    class UploadValidationTests {

        @Test
        @DisplayName("空文件应抛出异常")
        void shouldRejectEmptyFile() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    bookService.upload(userId, file, null, null, null, 1)
            );

            assertEquals("请选择文件", ex.getMessage());
            verify(bookMapper, never()).insert(any());
        }

        @Test
        @DisplayName("文件小于1MB应抛出异常")
        void shouldRejectSmallFile() throws IOException {
            byte[] smallContent = new byte[500 * 1024]; // 500KB
            MultipartFile file = createMockFile("test.pdf", smallContent);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    bookService.upload(userId, file, null, null, null, 1)
            );

            assertEquals("文件大小不能小于1MB", ex.getMessage());
        }

        @Test
        @DisplayName("文件超过150MB应抛出异常")
        void shouldRejectOversizedFile() throws IOException {
            ReflectionTestUtils.setField(bookService, "maxFileSize", 2 * 1024 * 1024L); // 2MB for test
            byte[] bigContent = new byte[3 * 1024 * 1024]; // 3MB
            MultipartFile file = createMockFile("test.pdf", bigContent);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    bookService.upload(userId, file, null, null, null, 1)
            );

            assertEquals("文件大小不能超过150MB", ex.getMessage());
        }

        @Test
        @DisplayName("非PDF文件应抛出异常")
        void shouldRejectNonPdfFile() throws IOException {
            byte[] content = new byte[2 * 1024 * 1024];
            MultipartFile file = createMockFile("test.txt", content);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    bookService.upload(userId, file, null, null, null, 1)
            );

            assertEquals("仅支持PDF文件", ex.getMessage());
        }

        @Test
        @DisplayName("PDF文件名大小写不敏感")
        void shouldAcceptPdfCaseInsensitive() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.PDF", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertNotNull(book);
            assertEquals("test", book.getTitle());
        }
    }

    @Nested
    @DisplayName("上传逻辑 - 版权声明")
    class UploadCopyrightTests {

        @Test
        @DisplayName("已声明版权应设置 copyrightDeclared = 1")
        void shouldSetCopyrightDeclaredTo1() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, "我的书", "作者", 1L, 1);

            assertEquals(1, book.getCopyrightDeclared());
            assertEquals("我的书", book.getTitle());
            assertEquals("作者", book.getAuthor());
            assertEquals(1L, book.getCategoryId());
            assertEquals(Constants.STATUS_ENABLED, book.getStatus());
        }

        @Test
        @DisplayName("未声明版权应设置 copyrightDeclared = 0")
        void shouldSetCopyrightDeclaredTo0() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, "我的书", null, null, 0);

            assertEquals(0, book.getCopyrightDeclared());
            assertEquals("", book.getAuthor());
            assertNull(book.getCategoryId());
        }

        @Test
        @DisplayName("copyrightDeclared 为 null 时应默认为 0")
        void shouldDefaultCopyrightDeclaredTo0WhenNull() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, "我的书", null, null, null);

            assertEquals(0, book.getCopyrightDeclared());
        }

        @Test
        @DisplayName("copyrightDeclared 为其他值时应设置为 0")
        void shouldSetTo0ForOtherValues() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, "我的书", null, null, 2);

            assertEquals(0, book.getCopyrightDeclared());
        }

        @Test
        @DisplayName("无标题时应使用文件名作为书名")
        void shouldUseFileNameAsTitle() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("Python编程指南.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertEquals("Python编程指南", book.getTitle());
        }
    }

    @Nested
    @DisplayName("上传逻辑 - 文件处理")
    class UploadFileProcessingTests {

        @Test
        @DisplayName("应正确获取PDF页数")
        void shouldGetCorrectPageCount() throws Exception {
            int expectedPages = 10;
            byte[] pdfContent = createValidPdf(expectedPages);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertEquals(expectedPages, book.getPageCount());
            assertEquals(pdfContent.length, book.getFileSize());
        }

        @Test
        @DisplayName("应正确设置文件路径")
        void shouldSetCorrectFilePath() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertTrue(book.getFilePath().startsWith(userId + "/"));
            assertTrue(book.getFilePath().endsWith(".pdf"));
            assertEquals(userId, book.getUserId());
        }

        @Test
        @DisplayName("应保存文件到用户目录")
        void shouldSaveFileToUserDirectory() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            bookService.upload(userId, file, null, null, null, 1);

            Path userDir = tempDir.resolve(userId.toString());
            assertTrue(Files.exists(userDir));
            assertTrue(Files.isDirectory(userDir));

            long fileCount = Files.list(userDir).count();
            assertEquals(1, fileCount);
        }

        @Test
        @DisplayName("文件名应为UUID格式")
        void shouldUseUuidFileName() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            String fileName = book.getFilePath().substring(book.getFilePath().lastIndexOf('/') + 1);
            assertEquals(40, fileName.length()); // UUID(36) + .pdf(4) = 40
            assertTrue(fileName.endsWith(".pdf"));
        }
    }

    @Nested
    @DisplayName("上传逻辑 - 书名审核")
    class UploadAuditTests {

        @Test
        @DisplayName("上传后应自动触发书名审核")
        void shouldTriggerTitleAuditAfterUpload() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);
            String title = "正常书名";

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(100L);
                return 1;
            });

            bookService.upload(userId, file, title, null, null, 1);

            verify(contentComplianceService, times(1)).auditText(
                    eq(Constants.AUDIT_TARGET_BOOK_TITLE),
                    eq(100L),
                    eq(title)
            );
        }

        @Test
        @DisplayName("书名审核失败不应影响上传")
        void shouldNotFailUploadWhenAuditFails() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            doThrow(new RuntimeException("审核服务异常"))
                    .when(contentComplianceService)
                    .auditText(anyInt(), anyLong(), anyString());

            Book book = bookService.upload(userId, file, "书名", null, null, 1);

            assertNotNull(book);
            assertEquals(1, book.getId());
        }

        @Test
        @DisplayName("含敏感词书名应被检测")
        void shouldDetectSensitiveTitle() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);
            String sensitiveTitle = "暴力教程";

            ContentAudit audit = new ContentAudit();
            audit.setResult(Constants.AUDIT_RESULT_SUSPECTED);
            audit.setKeywords("暴力");

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            when(contentComplianceService.auditText(
                    eq(Constants.AUDIT_TARGET_BOOK_TITLE),
                    eq(1L),
                    eq(sensitiveTitle)
            )).thenReturn(audit);

            Book book = bookService.upload(userId, file, sensitiveTitle, null, null, 1);

            assertNotNull(book);
            assertEquals(sensitiveTitle, book.getTitle());
            verify(contentComplianceService, times(1)).auditText(
                    eq(Constants.AUDIT_TARGET_BOOK_TITLE), eq(1L), eq(sensitiveTitle)
            );
        }
    }

    @Nested
    @DisplayName("上传逻辑 - 异常处理")
    class UploadExceptionTests {

        @Test
        @DisplayName("PDF解析失败应抛出业务异常")
        void shouldHandlePdfParseError() throws Exception {
            byte[] invalidPdf = new byte[2 * 1024 * 1024];
            MultipartFile file = createMockFile("test.pdf", invalidPdf);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    bookService.upload(userId, file, null, null, null, 1)
            );

            assertTrue(ex.getMessage().startsWith("文件上传失败"));
        }

        @Test
        @DisplayName("文件IO错误应抛出业务异常")
        void shouldHandleIoError() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn((long) pdfContent.length);
            when(file.getOriginalFilename()).thenReturn("test.pdf");
            when(file.getInputStream()).thenThrow(new IOException("磁盘IO错误"));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    bookService.upload(userId, file, null, null, null, 1)
            );

            assertTrue(ex.getMessage().startsWith("文件上传失败"));
        }

        @Test
        @DisplayName("文件名为 null 时应使用默认标题")
        void shouldHandleNullOriginalFilename() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn((long) pdfContent.length);
            when(file.getOriginalFilename()).thenReturn(null);
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(pdfContent));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    bookService.upload(userId, file, null, null, null, 1)
            );

            assertEquals("仅支持PDF文件", ex.getMessage());
            verify(bookMapper, never()).insert(any());
        }

        @Test
        @DisplayName("空字符串标题应使用文件名")
        void shouldUseFilenameWhenTitleIsEmpty() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("默认书名.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, "", null, null, 1);

            assertEquals("默认书名", book.getTitle());
        }

        @Test
        @DisplayName("空白字符串标题应使用文件名")
        void shouldUseFilenameWhenTitleIsBlank() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("空白标题.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, "   ", null, null, 1);

            assertEquals("空白标题", book.getTitle());
        }
    }

    @Nested
    @DisplayName("上传逻辑 - 字段默认值")
    class UploadDefaultValueTests {

        @Test
        @DisplayName("初始阅读进度应为0")
        void shouldSetLastPageTo0() throws Exception {
            byte[] pdfContent = createValidPdf(10);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertEquals(0, book.getLastPage());
        }

        @Test
        @DisplayName("状态应为启用")
        void shouldSetStatusToEnabled() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertEquals(Constants.STATUS_ENABLED, book.getStatus());
        }

        @Test
        @DisplayName("作者为 null 时应设为空字符串")
        void shouldSetAuthorToEmptyStringWhenNull() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, "测试书籍", null, null, 1);

            assertEquals("", book.getAuthor());
        }

        @Test
        @DisplayName("分类ID为 null 时应保持 null")
        void shouldKeepCategoryIdNull() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertNull(book.getCategoryId());
        }

        @Test
        @DisplayName("文件路径应包含用户ID和UUID")
        void shouldHaveCorrectFilePathFormat() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            String filePath = book.getFilePath();
            String[] parts = filePath.split("/");

            assertEquals(2, parts.length);
            assertEquals(userId.toString(), parts[0]);
            assertTrue(parts[1].endsWith(".pdf"));
            assertEquals(40, parts[1].length());
        }
    }

    @Nested
    @DisplayName("上传逻辑 - 文件名处理")
    class UploadFilenameProcessingTests {

        @Test
        @DisplayName("文件名包含多个点号应正确处理")
        void shouldHandleFilenameWithMultipleDots() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("Java.编程.指南.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertEquals("Java.编程.指南", book.getTitle());
        }

        @Test
        @DisplayName("文件名包含特殊字符应正确处理")
        void shouldHandleSpecialCharactersInFilename() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("【推荐】Python入门到精通.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertEquals("【推荐】Python入门到精通", book.getTitle());
        }

        @Test
        @DisplayName("文件名包含空格应正确处理")
        void shouldHandleSpacesInFilename() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("C++ 高级编程 第三版.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertEquals("C++ 高级编程 第三版", book.getTitle());
        }

        @Test
        @DisplayName("文件恰好等于边界大小应允许上传")
        void shouldAcceptFileAtExactBoundary() throws Exception {
            int boundarySize = 1024 * 1024 + 100;
            byte[] pdfContent = createValidPdf(5);
            assertEquals(boundarySize, pdfContent.length);

            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertNotNull(book);
            verify(bookMapper, times(1)).insert(any());
        }

        @Test
        @DisplayName(".PDF 大写后缀应正确去除")
        void shouldRemoveUpperCasePdfExtension() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("TEST.PDF", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertEquals("TEST", book.getTitle());
        }

        @Test
        @DisplayName(".Pdf 混合大小写后缀应正确去除")
        void shouldRemoveMixedCasePdfExtension() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("TestBook.Pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertEquals("TestBook", book.getTitle());
        }

        @Test
        @DisplayName("文件名为 .pdf 时应使用默认标题")
        void shouldHandleFilenameIsOnlyPdf() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile(".pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertEquals("", book.getTitle());
        }
    }

    @Nested
    @DisplayName("上传逻辑 - 多用户隔离")
    class UploadMultiUserTests {

        @Test
        @DisplayName("不同用户应保存到不同目录")
        void shouldSaveToDifferentDirectoriesForDifferentUsers() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file1 = createMockFile("test1.pdf", pdfContent);
            MultipartFile file2 = createMockFile("test2.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book1 = bookService.upload(100L, file1, null, null, null, 1);
            Book book2 = bookService.upload(200L, file2, null, null, null, 1);

            assertTrue(book1.getFilePath().startsWith("100/"));
            assertTrue(book2.getFilePath().startsWith("200/"));
            assertNotEquals(book1.getFilePath(), book2.getFilePath());
        }

        @Test
        @DisplayName("用户ID应正确设置")
        void shouldSetCorrectUserId() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);
            Long testUserId = 999L;

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(testUserId, file, null, null, null, 1);

            assertEquals(testUserId, book.getUserId());
        }
    }

    @Nested
    @DisplayName("上传逻辑 - 边界值测试")
    class UploadBoundaryTests {

        @Test
        @DisplayName("文件恰好等于最小大小应允许上传")
        void shouldAcceptFileAtMinSize() throws Exception {
            byte[] pdfContent = createValidPdfWithSize(1024 * 1024); // 恰好1MB

            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertNotNull(book);
            assertEquals(1024 * 1024, book.getFileSize());
        }

        @Test
        @DisplayName("文件比最小大小小1字节应拒绝")
        void shouldRejectFileOneByteBelowMinSize() throws Exception {
            byte[] pdfContent = createValidPdfWithSize(1024 * 1024 - 1); // 1MB - 1 byte

            MultipartFile file = createMockFile("test.pdf", pdfContent);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    bookService.upload(userId, file, null, null, null, 1)
            );

            assertEquals("文件大小不能小于1MB", ex.getMessage());
        }

        @Test
        @DisplayName("文件恰好等于最大大小应允许上传")
        void shouldAcceptFileAtMaxSize() throws Exception {
            long maxSize = 150 * 1024 * 1024L;
            byte[] pdfContent = createValidPdfWithSize((int) maxSize);

            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, null, null, null, 1);

            assertNotNull(book);
            assertEquals(maxSize, book.getFileSize());
        }

        @Test
        @DisplayName("空作者应正确处理")
        void shouldHandleEmptyAuthor() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, "测试书籍", "", null, 1);

            assertEquals("", book.getAuthor());
        }

        @Test
        @DisplayName("空白作者应正确处理")
        void shouldHandleBlankAuthor() throws Exception {
            byte[] pdfContent = createValidPdf(5);
            MultipartFile file = createMockFile("test.pdf", pdfContent);

            when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(1L);
                return 1;
            });

            Book book = bookService.upload(userId, file, "测试书籍", "   ", null, 1);

            assertEquals("   ", book.getAuthor());
        }
    }

    private MultipartFile createMockFile(String filename, byte[] content) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) content.length);
        when(file.getOriginalFilename()).thenReturn(filename);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        return file;
    }

    private byte[] createValidPdf(int pageCount) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contentStream.newLineAtOffset(100, 700);
                    contentStream.showText("Page " + (i + 1));
                    contentStream.endText();
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);

            byte[] pdfBytes = baos.toByteArray();
            if (pdfBytes.length < 1024 * 1024) {
                byte[] padded = new byte[1024 * 1024 + 100];
                System.arraycopy(pdfBytes, 0, padded, 0, pdfBytes.length);
                return padded;
            }
            return pdfBytes;
        }
    }

    private byte[] createValidPdfWithSize(int targetSize) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("Test Page");
                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);

            byte[] pdfBytes = baos.toByteArray();
            if (pdfBytes.length >= targetSize) {
                return pdfBytes;
            }
            byte[] padded = new byte[targetSize];
            System.arraycopy(pdfBytes, 0, padded, 0, pdfBytes.length);
            return padded;
        }
    }
}
