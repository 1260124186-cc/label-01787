package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.dto.CopyrightComplaintDTO;
import com.xiaoan.bookstore.entity.*;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentComplianceService 单元测试")
class ContentComplianceServiceTest {

    @Mock
    private ContentAuditMapper contentAuditMapper;

    @Mock
    private CopyrightComplaintMapper copyrightComplaintMapper;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private ContentComplianceService contentComplianceService;

    private final LocalDateTime now = LocalDateTime.now();

    @Nested
    @DisplayName("敏感词检测 - auditText")
    class AuditTextTests {

        @Test
        @DisplayName("正常文本应返回通过结果")
        void shouldReturnPassForNormalText() {
            when(contentAuditMapper.insert(any(ContentAudit.class))).thenReturn(1);

            ContentAudit audit = contentComplianceService.auditText(1, 1L, "这是一本很好的编程书籍");

            assertNotNull(audit);
            assertEquals(Constants.AUDIT_RESULT_PASS, audit.getResult());
            assertEquals("", audit.getKeywords());
            verify(contentAuditMapper, times(1)).insert(any(ContentAudit.class));
        }

        @Test
        @DisplayName("含敏感词文本应返回疑似或违规结果")
        void shouldDetectSensitiveWords() {
            when(contentAuditMapper.insert(any(ContentAudit.class))).thenReturn(1);

            ContentAudit audit = contentComplianceService.auditText(1, 1L, "这里有暴力内容");

            assertNotNull(audit);
            assertTrue(audit.getResult() >= Constants.AUDIT_RESULT_SUSPECTED);
            assertTrue(audit.getKeywords().contains("暴力"));
        }

        @Test
        @DisplayName("多个敏感词应返回违规结果")
        void shouldReturnViolationForMultipleSensitiveWords() {
            when(contentAuditMapper.insert(any(ContentAudit.class))).thenReturn(1);

            ContentAudit audit = contentComplianceService.auditText(
                    1, 1L, "暴力色情赌博内容都包含了"
            );

            assertEquals(Constants.AUDIT_RESULT_VIOLATION, audit.getResult());
            assertTrue(audit.getKeywords().contains("暴力"));
            assertTrue(audit.getKeywords().contains("色情"));
            assertTrue(audit.getKeywords().contains("赌博"));
        }

        @Test
        @DisplayName("长文本应截断保存")
        void shouldTruncateLongText() {
            when(contentAuditMapper.insert(any(ContentAudit.class))).thenReturn(1);
            String longText = "a".repeat(1000);

            ContentAudit audit = contentComplianceService.auditText(1, 1L, longText);

            assertTrue(audit.getContent().length() <= 500);
        }

        @Test
        @DisplayName("不区分大小写检测敏感词")
        void shouldBeCaseInsensitive() {
            when(contentAuditMapper.insert(any(ContentAudit.class))).thenReturn(1);

            ContentAudit audit = contentComplianceService.auditText(1, 1L, "BAOLI 暴力");

            assertTrue(audit.getResult() > 0);
        }
    }

    @Nested
    @DisplayName("版权申诉提交 - submitComplaint")
    class SubmitComplaintTests {

        @Test
        @DisplayName("应成功提交版权申诉")
        void shouldSubmitComplaintSuccessfully() {
            CopyrightComplaintDTO dto = new CopyrightComplaintDTO();
            dto.setComplainantName("张三");
            dto.setComplainantContact("13800138000");
            dto.setBookId(100L);
            dto.setBookTitle("Python编程从入门到精通");
            dto.setReason("未经授权传播我的作品");
            dto.setEvidenceUrls("https://example.com/proof.pdf");

            when(copyrightComplaintMapper.insert(any(CopyrightComplaint.class))).thenReturn(1);

            CopyrightComplaint complaint = contentComplianceService.submitComplaint(dto);

            assertNotNull(complaint);
            assertEquals("张三", complaint.getComplainantName());
            assertEquals("13800138000", complaint.getComplainantContact());
            assertEquals(100L, complaint.getBookId());
            assertEquals("Python编程从入门到精通", complaint.getBookTitle());
            assertEquals("未经授权传播我的作品", complaint.getReason());
            assertEquals(Constants.COMPLAINT_PENDING, complaint.getStatus());
            verify(copyrightComplaintMapper, times(1)).insert(any(CopyrightComplaint.class));
        }

        @Test
        @DisplayName("未指定 bookId 时应保存为空")
        void shouldHandleNullBookId() {
            CopyrightComplaintDTO dto = new CopyrightComplaintDTO();
            dto.setComplainantName("张三");
            dto.setComplainantContact("13800138000");
            dto.setBookTitle("未知书籍");
            dto.setReason("侵权");

            when(copyrightComplaintMapper.insert(any(CopyrightComplaint.class))).thenReturn(1);

            CopyrightComplaint complaint = contentComplianceService.submitComplaint(dto);

            assertNull(complaint.getBookId());
            assertEquals("未知书籍", complaint.getBookTitle());
        }

        @Test
        @DisplayName("空值字段应处理为空字符串")
        void shouldHandleNullFields() {
            CopyrightComplaintDTO dto = new CopyrightComplaintDTO();
            dto.setComplainantName("张三");
            dto.setComplainantContact("13800138000");
            dto.setReason("侵权");
            dto.setBookTitle(null);
            dto.setEvidenceUrls(null);

            when(copyrightComplaintMapper.insert(any(CopyrightComplaint.class))).thenReturn(1);

            CopyrightComplaint complaint = contentComplianceService.submitComplaint(dto);

            assertEquals("", complaint.getBookTitle());
            assertEquals("", complaint.getEvidenceUrls());
        }
    }

    @Nested
    @DisplayName("版权申诉处理 - handleComplaint")
    class HandleComplaintTests {

        private CopyrightComplaint pendingComplaint;

        @BeforeEach
        void setUp() {
            pendingComplaint = new CopyrightComplaint();
            pendingComplaint.setId(1L);
            pendingComplaint.setStatus(Constants.COMPLAINT_PENDING);
            pendingComplaint.setBookId(100L);
            pendingComplaint.setBookTitle("测试书籍");
        }

        @Test
        @DisplayName("关联书籍的工单应成功下架")
        void shouldTakeDownBookWithValidBookId() {
            Book book = new Book();
            book.setId(100L);
            book.setStatus(Constants.STATUS_ENABLED);
            book.setTitle("测试书籍");

            when(copyrightComplaintMapper.selectById(1L)).thenReturn(pendingComplaint);
            when(bookMapper.selectById(100L)).thenReturn(book);
            when(copyrightComplaintMapper.updateById(any())).thenReturn(1);
            when(bookMapper.updateById(any())).thenReturn(1);

            contentComplianceService.handleComplaint(
                    1L, Constants.COMPLAINT_TAKEN_DOWN, "侵权属实，已下架", 1L, null
            );

            verify(copyrightComplaintMapper, times(1)).updateById(
                    argThat(c -> c.getStatus() == Constants.COMPLAINT_TAKEN_DOWN
                            && "侵权属实，已下架".equals(c.getHandleResult())
                            && 1L == c.getHandlerId())
            );
            verify(bookMapper, times(1)).updateById(
                    argThat(b -> b.getStatus() == Constants.STATUS_TAKEN_DOWN)
            );
        }

        @Test
        @DisplayName("未关联书籍且下架时应抛出异常")
        void shouldThrowExceptionWhenTakingDownWithoutBookId() {
            pendingComplaint.setBookId(null);
            pendingComplaint.setBookTitle("未知书籍");

            when(copyrightComplaintMapper.selectById(1L)).thenReturn(pendingComplaint);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    contentComplianceService.handleComplaint(
                            1L, Constants.COMPLAINT_TAKEN_DOWN, "下架", 1L, null
                    )
            );

            assertEquals("下架操作必须关联书籍，请先选择要下架的书籍", ex.getMessage());
            verify(bookMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("传入 bookId 时应先关联再下架")
        void shouldAssociateBookIdWhenProvided() {
            pendingComplaint.setBookId(null);
            pendingComplaint.setBookTitle("未知书籍");

            Book book = new Book();
            book.setId(200L);
            book.setStatus(Constants.STATUS_ENABLED);
            book.setTitle("实际侵权书籍");

            when(copyrightComplaintMapper.selectById(1L)).thenReturn(pendingComplaint);
            when(bookMapper.selectById(200L)).thenReturn(book);
            when(copyrightComplaintMapper.updateById(any())).thenReturn(1);
            when(bookMapper.updateById(any())).thenReturn(1);

            contentComplianceService.handleComplaint(
                    1L, Constants.COMPLAINT_TAKEN_DOWN, "下架", 1L, 200L
            );

            verify(copyrightComplaintMapper, times(1)).updateById(
                    argThat(c -> c.getBookId() == 200L
                            && "实际侵权书籍".equals(c.getBookTitle())
                            && c.getStatus() == Constants.COMPLAINT_TAKEN_DOWN)
            );
            verify(bookMapper, times(1)).updateById(
                    argThat(b -> b.getId() == 200L && b.getStatus() == Constants.STATUS_TAKEN_DOWN)
            );
        }

        @Test
        @DisplayName("传入不存在的 bookId 应抛出异常")
        void shouldThrowExceptionWhenBookIdNotExists() {
            when(copyrightComplaintMapper.selectById(1L)).thenReturn(pendingComplaint);
            when(bookMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    contentComplianceService.handleComplaint(
                            1L, Constants.COMPLAINT_TAKEN_DOWN, "下架", 1L, 999L
                    )
            );

            assertEquals("关联的书籍不存在", ex.getMessage());
            verify(bookMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("已下架的书籍不应重复更新")
        void shouldNotUpdateAlreadyTakenDownBook() {
            Book book = new Book();
            book.setId(100L);
            book.setStatus(Constants.STATUS_TAKEN_DOWN);

            when(copyrightComplaintMapper.selectById(1L)).thenReturn(pendingComplaint);
            when(bookMapper.selectById(100L)).thenReturn(book);
            when(copyrightComplaintMapper.updateById(any())).thenReturn(1);

            contentComplianceService.handleComplaint(
                    1L, Constants.COMPLAINT_TAKEN_DOWN, "下架", 1L, null
            );

            verify(bookMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("驳回申诉不需要 bookId")
        void shouldRejectComplaintWithoutBookId() {
            pendingComplaint.setBookId(null);

            when(copyrightComplaintMapper.selectById(1L)).thenReturn(pendingComplaint);
            when(copyrightComplaintMapper.updateById(any())).thenReturn(1);

            contentComplianceService.handleComplaint(
                    1L, Constants.COMPLAINT_REJECTED, "申诉不成立", 1L, null
            );

            verify(copyrightComplaintMapper, times(1)).updateById(
                    argThat(c -> c.getStatus() == Constants.COMPLAINT_REJECTED)
            );
            verify(bookMapper, never()).selectById(any());
        }

        @Test
        @DisplayName("已处理的工单不能重复处理")
        void shouldNotHandleAlreadyProcessedComplaint() {
            pendingComplaint.setStatus(Constants.COMPLAINT_TAKEN_DOWN);

            when(copyrightComplaintMapper.selectById(1L)).thenReturn(pendingComplaint);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    contentComplianceService.handleComplaint(
                            1L, Constants.COMPLAINT_TAKEN_DOWN, "下架", 1L, null
                    )
            );

            assertEquals("该工单已处理完毕", ex.getMessage());
            verify(copyrightComplaintMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("不存在的工单应抛出异常")
        void shouldThrowExceptionForNonExistentComplaint() {
            when(copyrightComplaintMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    contentComplianceService.handleComplaint(
                            999L, Constants.COMPLAINT_TAKEN_DOWN, "下架", 1L, null
                    )
            );

            assertEquals("申诉工单不存在", ex.getMessage());
        }

        @Test
        @DisplayName("标记为处理中不需要 bookId")
        void shouldMarkProcessingWithoutBookId() {
            pendingComplaint.setBookId(null);

            when(copyrightComplaintMapper.selectById(1L)).thenReturn(pendingComplaint);
            when(copyrightComplaintMapper.updateById(any())).thenReturn(1);

            contentComplianceService.handleComplaint(
                    1L, Constants.COMPLAINT_PROCESSING, "正在核实", 1L, null
            );

            verify(copyrightComplaintMapper, times(1)).updateById(
                    argThat(c -> c.getStatus() == Constants.COMPLAINT_PROCESSING)
            );
        }
    }

    @Nested
    @DisplayName("列表查询 - complaintList & auditList")
    class ListQueryTests {

        @Test
        @DisplayName("申诉列表应正确分页")
        void shouldGetComplaintList() {
            Page<CopyrightComplaint> page = new Page<>(1, 10);
            page.setRecords(List.of(new CopyrightComplaint()));
            page.setTotal(1L);

            when(copyrightComplaintMapper.selectPage(
                    any(Page.class), any(LambdaQueryWrapper.class)
            )).thenReturn(page);

            Page<CopyrightComplaint> result = contentComplianceService.complaintList(1, 10, null);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("申诉列表应按状态过滤")
        void shouldFilterComplaintByStatus() {
            when(copyrightComplaintMapper.selectPage(
                    any(Page.class), any(LambdaQueryWrapper.class)
            )).thenReturn(new Page<>());

            contentComplianceService.complaintList(1, 10, Constants.COMPLAINT_PENDING);

            verify(copyrightComplaintMapper).selectPage(
                    any(Page.class),
                    argThat(wrapper -> wrapper.getSqlSegment().contains("status = " + Constants.COMPLAINT_PENDING))
            );
        }

        @Test
        @DisplayName("审核列表应正确分页")
        void shouldGetAuditList() {
            Page<ContentAudit> page = new Page<>(1, 20);
            page.setRecords(List.of(new ContentAudit()));
            page.setTotal(1L);

            when(contentAuditMapper.selectPage(
                    any(Page.class), any(LambdaQueryWrapper.class)
            )).thenReturn(page);

            Page<ContentAudit> result = contentComplianceService.auditList(1, 20, null, null);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("审核列表应按结果和目标类型过滤")
        void shouldFilterAuditByResultAndTargetType() {
            when(contentAuditMapper.selectPage(
                    any(Page.class), any(LambdaQueryWrapper.class)
            )).thenReturn(new Page<>());

            contentComplianceService.auditList(1, 20, Constants.AUDIT_RESULT_VIOLATION, Constants.AUDIT_TARGET_BOOK_TITLE);

            verify(contentAuditMapper).selectPage(
                    any(Page.class),
                    argThat(wrapper -> {
                        String sql = wrapper.getSqlSegment();
                        return sql.contains("result = " + Constants.AUDIT_RESULT_VIOLATION)
                                && sql.contains("target_type = " + Constants.AUDIT_TARGET_BOOK_TITLE);
                    })
            );
        }
    }

    @Nested
    @DisplayName("合规审计报告 - generateComplianceReport")
    class ComplianceReportTests {

        private final LocalDateTime startTime = LocalDateTime.now().minusDays(30);
        private final LocalDateTime endTime = LocalDateTime.now();

        @Test
        @DisplayName("应生成完整的合规报告")
        void shouldGenerateFullReport() {
            when(contentAuditMapper.countByResultAndTimeRange(eq(Constants.AUDIT_RESULT_PASS), any(), any()))
                    .thenReturn(100L);
            when(contentAuditMapper.countByResultAndTimeRange(eq(Constants.AUDIT_RESULT_SUSPECTED), any(), any()))
                    .thenReturn(5L);
            when(contentAuditMapper.countByResultAndTimeRange(eq(Constants.AUDIT_RESULT_VIOLATION), any(), any()))
                    .thenReturn(3L);

            when(copyrightComplaintMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(10L, 4L, 3L, 3L);

            when(bookMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(2L, 80L, 100L);

            when(operationLogMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(500L);

            Map<String, Object> report = contentComplianceService.generateComplianceReport(startTime, endTime);

            assertNotNull(report);
            assertTrue(report.containsKey("period"));
            assertTrue(report.containsKey("contentAudit"));
            assertTrue(report.containsKey("copyrightComplaint"));
            assertTrue(report.containsKey("bookCompliance"));
            assertTrue(report.containsKey("operationAudit"));
            assertTrue(report.containsKey("generatedAt"));

            @SuppressWarnings("unchecked")
            Map<String, Object> contentAudit = (Map<String, Object>) report.get("contentAudit");
            assertEquals(108L, contentAudit.get("total"));
            assertEquals(100L, contentAudit.get("pass"));
            assertEquals(5L, contentAudit.get("suspected"));
            assertEquals(3L, contentAudit.get("violation"));
            assertEquals("92.6%", contentAudit.get("passRate"));

            @SuppressWarnings("unchecked")
            Map<String, Object> bookCompliance = (Map<String, Object>) report.get("bookCompliance");
            assertEquals(100L, bookCompliance.get("totalUploaded"));
            assertEquals(80L, bookCompliance.get("copyrightDeclared"));
            assertEquals(2L, bookCompliance.get("takenDown"));
            assertEquals("80.0%", bookCompliance.get("copyrightRate"));

            @SuppressWarnings("unchecked")
            Map<String, Object> operationAudit = (Map<String, Object>) report.get("operationAudit");
            assertEquals(500L, operationAudit.get("totalOperations"));
        }

        @Test
        @DisplayName("无数据时通过率应显示 N/A")
        void shouldShowNAWhenNoAuditData() {
            when(contentAuditMapper.countByResultAndTimeRange(anyInt(), any(), any()))
                    .thenReturn(0L);
            when(copyrightComplaintMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L);
            when(bookMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L);
            when(operationLogMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L);

            Map<String, Object> report = contentComplianceService.generateComplianceReport(startTime, endTime);

            @SuppressWarnings("unchecked")
            Map<String, Object> contentAudit = (Map<String, Object>) report.get("contentAudit");
            assertEquals("N/A", contentAudit.get("passRate"));

            @SuppressWarnings("unchecked")
            Map<String, Object> bookCompliance = (Map<String, Object>) report.get("bookCompliance");
            assertEquals("N/A", bookCompliance.get("copyrightRate"));
        }

        @Test
        @DisplayName("应正确处理 null 返回值")
        void shouldHandleNullReturnValues() {
            when(contentAuditMapper.countByResultAndTimeRange(anyInt(), any(), any()))
                    .thenReturn(null);
            when(copyrightComplaintMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(null);
            when(bookMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(null);
            when(operationLogMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(null);

            Map<String, Object> report = contentComplianceService.generateComplianceReport(startTime, endTime);

            @SuppressWarnings("unchecked")
            Map<String, Object> contentAudit = (Map<String, Object>) report.get("contentAudit");
            assertEquals(0L, contentAudit.get("total"));
            assertEquals("N/A", contentAudit.get("passRate"));
        }

        @Test
        @DisplayName("应包含正确的时间范围")
        void shouldIncludeCorrectPeriod() {
            when(contentAuditMapper.countByResultAndTimeRange(anyInt(), any(), any()))
                    .thenReturn(0L);
            when(copyrightComplaintMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L);
            when(bookMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L);
            when(operationLogMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L);

            Map<String, Object> report = contentComplianceService.generateComplianceReport(startTime, endTime);

            @SuppressWarnings("unchecked")
            Map<String, String> period = (Map<String, String>) report.get("period");
            assertEquals(startTime.toString(), period.get("start"));
            assertEquals(endTime.toString(), period.get("end"));
        }
    }

    @Nested
    @DisplayName("工具方法 - nullToZero")
    class NullToZeroTests {

        @Test
        @DisplayName("null 应转换为 0")
        void shouldConvertNullToZero() {
            assertEquals(0L, invokeNullToZero(null));
        }

        @Test
        @DisplayName("非 null 值应保持不变")
        void shouldKeepNonNullValue() {
            assertEquals(0L, invokeNullToZero(0L));
            assertEquals(100L, invokeNullToZero(100L));
            assertEquals(-50L, invokeNullToZero(-50L));
        }

        private long invokeNullToZero(Long value) {
            try {
                java.lang.reflect.Method method = ContentComplianceService.class.getDeclaredMethod("nullToZero", Long.class);
                method.setAccessible(true);
                return (long) method.invoke(contentComplianceService, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    @DisplayName("上传书籍时应自动触发书名审核")
    void shouldAuditBookTitleOnUpload() {
        ContentAudit audit = new ContentAudit();
        audit.setResult(Constants.AUDIT_RESULT_PASS);

        when(contentAuditMapper.insert(any(ContentAudit.class))).thenReturn(1);

        contentComplianceService.auditText(Constants.AUDIT_TARGET_BOOK_TITLE, 1L, "正常书名");

        verify(contentAuditMapper, times(1)).insert(
                argThat(a -> a.getTargetType() == Constants.AUDIT_TARGET_BOOK_TITLE
                        && a.getTargetId() == 1L
                        && "正常书名".equals(a.getContent()))
        );
    }

    @Test
    @DisplayName("添加批注时应自动触发内容审核")
    void shouldAuditAnnotationContent() {
        String content = "批注内容";
        String selectedText = "选中文本";
        String fullContent = selectedText + " " + content;

        when(contentAuditMapper.insert(any(ContentAudit.class))).thenReturn(1);

        contentComplianceService.auditText(Constants.AUDIT_TARGET_ANNOTATION, 1L, fullContent);

        verify(contentAuditMapper, times(1)).insert(
                argThat(a -> a.getTargetType() == Constants.AUDIT_TARGET_ANNOTATION
                        && a.getTargetId() == 1L
                        && a.getContent().contains(content)
                        && a.getContent().contains(selectedText))
        );
    }

    @Test
    @DisplayName("版权申诉统计包含所有状态")
    void shouldIncludeAllStatusInComplaintStats() {
        when(contentAuditMapper.countByResultAndTimeRange(anyInt(), any(), any()))
                .thenReturn(0L);
        when(copyrightComplaintMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(15L, 5L, 7L, 3L);
        when(bookMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L);
        when(operationLogMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L);

        Map<String, Object> report = contentComplianceService.generateComplianceReport(
                LocalDateTime.now().minusDays(30), LocalDateTime.now()
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> complaintStats = (Map<String, Object>) report.get("copyrightComplaint");

        assertEquals(15L, complaintStats.get("total"));
        assertEquals(5L, complaintStats.get("pending"));
        assertEquals(7L, complaintStats.get("takenDown"));
        assertEquals(3L, complaintStats.get("rejected"));
    }
}
