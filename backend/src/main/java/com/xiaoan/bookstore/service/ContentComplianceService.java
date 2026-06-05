package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.annotation.TenantIgnore;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.dto.CopyrightComplaintDTO;
import com.xiaoan.bookstore.entity.*;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@TenantIgnore
public class ContentComplianceService {

    private static final Logger log = LoggerFactory.getLogger(ContentComplianceService.class);

    private final ContentAuditMapper contentAuditMapper;
    private final CopyrightComplaintMapper copyrightComplaintMapper;
    private final BookMapper bookMapper;
    private final OperationLogMapper operationLogMapper;

    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "暴力", "色情", "赌博", "毒品", "诈骗", "传销",
            "邪教", "恐怖", "分裂", "仇恨", "歧视", "血腥",
            "自杀", "自残", "虐待", "枪支", "弹药", "爆炸",
            "洗钱", "走私", "卖淫", "嫖娼", "非法", "违禁"
    );

    private static final Pattern SENSITIVE_PATTERN = buildSensitivePattern();

    private static Pattern buildSensitivePattern() {
        String joined = String.join("|", SENSITIVE_WORDS);
        return Pattern.compile("(" + joined + ")", Pattern.CASE_INSENSITIVE);
    }

    public ContentAudit auditText(int targetType, Long targetId, String content) {
        java.util.regex.Matcher matcher = SENSITIVE_PATTERN.matcher(content);
        List<String> hitWords = new ArrayList<>();
        while (matcher.find()) {
            String word = matcher.group(1);
            if (!hitWords.contains(word)) {
                hitWords.add(word);
            }
        }

        int result;
        String keywords;
        if (hitWords.isEmpty()) {
            result = Constants.AUDIT_RESULT_PASS;
            keywords = "";
        } else if (hitWords.size() <= 2) {
            result = Constants.AUDIT_RESULT_SUSPECTED;
            keywords = String.join(",", hitWords);
        } else {
            result = Constants.AUDIT_RESULT_VIOLATION;
            keywords = String.join(",", hitWords);
        }

        ContentAudit audit = new ContentAudit();
        audit.setTargetType(targetType);
        audit.setTargetId(targetId);
        audit.setContent(content.length() > 500 ? content.substring(0, 500) : content);
        audit.setResult(result);
        audit.setKeywords(keywords);
        audit.setCreatedAt(LocalDateTime.now());
        contentAuditMapper.insert(audit);

        log.info("内容审核: targetType={}, targetId={}, result={}, keywords={}", targetType, targetId, result, keywords);
        return audit;
    }

    public ContentAudit auditText(int targetType, Long targetId, String content, Long auditorId) {
        ContentAudit audit = auditText(targetType, targetId, content);
        audit.setAuditorId(auditorId);
        contentAuditMapper.updateById(audit);
        return audit;
    }

    public CopyrightComplaint submitComplaint(CopyrightComplaintDTO dto) {
        CopyrightComplaint complaint = new CopyrightComplaint();
        complaint.setComplainantName(dto.getComplainantName());
        complaint.setComplainantContact(dto.getComplainantContact());
        complaint.setBookId(dto.getBookId());
        complaint.setBookTitle(dto.getBookTitle() != null ? dto.getBookTitle() : "");
        complaint.setReason(dto.getReason());
        complaint.setEvidenceUrls(dto.getEvidenceUrls() != null ? dto.getEvidenceUrls() : "");
        complaint.setStatus(Constants.COMPLAINT_PENDING);
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());
        copyrightComplaintMapper.insert(complaint);

        log.info("版权申诉提交: id={}, bookTitle={}", complaint.getId(), complaint.getBookTitle());
        return complaint;
    }

    @Transactional
    public void handleComplaint(Long complaintId, Integer newStatus, String handleResult, Long handlerId, Long bookId) {
        CopyrightComplaint complaint = copyrightComplaintMapper.selectById(complaintId);
        if (complaint == null) {
            throw new BusinessException("申诉工单不存在");
        }
        if (complaint.getStatus() != Constants.COMPLAINT_PENDING && complaint.getStatus() != Constants.COMPLAINT_PROCESSING) {
            throw new BusinessException("该工单已处理完毕");
        }

        Long finalBookId = complaint.getBookId();
        if (bookId != null) {
            Book book = bookMapper.selectById(bookId);
            if (book == null) {
                throw new BusinessException("关联的书籍不存在");
            }
            finalBookId = bookId;
            complaint.setBookId(bookId);
            complaint.setBookTitle(book.getTitle());
        }

        if (newStatus == Constants.COMPLAINT_TAKEN_DOWN) {
            if (finalBookId == null) {
                throw new BusinessException("下架操作必须关联书籍，请先选择要下架的书籍");
            }
        }

        complaint.setStatus(newStatus);
        complaint.setHandleResult(handleResult);
        complaint.setHandlerId(handlerId);
        complaint.setHandledAt(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());
        copyrightComplaintMapper.updateById(complaint);

        if (newStatus == Constants.COMPLAINT_TAKEN_DOWN && finalBookId != null) {
            Book book = bookMapper.selectById(finalBookId);
            if (book != null && book.getStatus() == Constants.STATUS_ENABLED) {
                book.setStatus(Constants.STATUS_TAKEN_DOWN);
                bookMapper.updateById(book);
                log.info("书籍已下架: bookId={}, reason=complaint_{}", finalBookId, complaintId);
            }
        }

        log.info("版权申诉处理: complaintId={}, bookId={}, status={}, handlerId={}", complaintId, finalBookId, newStatus, handlerId);
    }

    public Page<CopyrightComplaint> complaintList(int page, int size, Integer status) {
        LambdaQueryWrapper<CopyrightComplaint> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(CopyrightComplaint::getStatus, status);
        }
        wrapper.orderByDesc(CopyrightComplaint::getCreatedAt);
        return copyrightComplaintMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<ContentAudit> auditList(int page, int size, Integer result, Integer targetType) {
        LambdaQueryWrapper<ContentAudit> wrapper = new LambdaQueryWrapper<>();
        if (result != null) {
            wrapper.eq(ContentAudit::getResult, result);
        }
        if (targetType != null) {
            wrapper.eq(ContentAudit::getTargetType, targetType);
        }
        wrapper.orderByDesc(ContentAudit::getCreatedAt);
        return contentAuditMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Map<String, Object> generateComplianceReport(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> report = new LinkedHashMap<>();

        report.put("period", Map.of("start", startTime.toString(), "end", endTime.toString()));

        long auditPass = nullToZero(contentAuditMapper.countByResultAndTimeRange(Constants.AUDIT_RESULT_PASS, startTime, endTime));
        long auditSuspected = nullToZero(contentAuditMapper.countByResultAndTimeRange(Constants.AUDIT_RESULT_SUSPECTED, startTime, endTime));
        long auditViolation = nullToZero(contentAuditMapper.countByResultAndTimeRange(Constants.AUDIT_RESULT_VIOLATION, startTime, endTime));
        long auditTotal = auditPass + auditSuspected + auditViolation;

        Map<String, Object> auditStats = new LinkedHashMap<>();
        auditStats.put("total", auditTotal);
        auditStats.put("pass", auditPass);
        auditStats.put("suspected", auditSuspected);
        auditStats.put("violation", auditViolation);
        auditStats.put("passRate", auditTotal > 0 ? String.format("%.1f%%", auditPass * 100.0 / auditTotal) : "N/A");
        report.put("contentAudit", auditStats);

        LambdaQueryWrapper<CopyrightComplaint> complaintWrapper = new LambdaQueryWrapper<>();
        complaintWrapper.ge(CopyrightComplaint::getCreatedAt, startTime);
        complaintWrapper.le(CopyrightComplaint::getCreatedAt, endTime);
        long complaintTotal = nullToZero(copyrightComplaintMapper.selectCount(complaintWrapper));

        LambdaQueryWrapper<CopyrightComplaint> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.ge(CopyrightComplaint::getCreatedAt, startTime);
        pendingWrapper.le(CopyrightComplaint::getCreatedAt, endTime);
        pendingWrapper.eq(CopyrightComplaint::getStatus, Constants.COMPLAINT_PENDING);
        long complaintPending = nullToZero(copyrightComplaintMapper.selectCount(pendingWrapper));

        LambdaQueryWrapper<CopyrightComplaint> takenDownWrapper = new LambdaQueryWrapper<>();
        takenDownWrapper.ge(CopyrightComplaint::getCreatedAt, startTime);
        takenDownWrapper.le(CopyrightComplaint::getCreatedAt, endTime);
        takenDownWrapper.eq(CopyrightComplaint::getStatus, Constants.COMPLAINT_TAKEN_DOWN);
        long complaintTakenDown = nullToZero(copyrightComplaintMapper.selectCount(takenDownWrapper));

        LambdaQueryWrapper<CopyrightComplaint> rejectedWrapper = new LambdaQueryWrapper<>();
        rejectedWrapper.ge(CopyrightComplaint::getCreatedAt, startTime);
        rejectedWrapper.le(CopyrightComplaint::getCreatedAt, endTime);
        rejectedWrapper.eq(CopyrightComplaint::getStatus, Constants.COMPLAINT_REJECTED);
        long complaintRejected = nullToZero(copyrightComplaintMapper.selectCount(rejectedWrapper));

        Map<String, Object> complaintStats = new LinkedHashMap<>();
        complaintStats.put("total", complaintTotal);
        complaintStats.put("pending", complaintPending);
        complaintStats.put("takenDown", complaintTakenDown);
        complaintStats.put("rejected", complaintRejected);
        report.put("copyrightComplaint", complaintStats);

        long bookTakenDown = nullToZero(bookMapper.selectCount(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, Constants.STATUS_TAKEN_DOWN)
                        .ge(Book::getUpdatedAt, startTime)
                        .le(Book::getUpdatedAt, endTime)
        ));
        long bookWithCopyright = nullToZero(bookMapper.selectCount(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getCopyrightDeclared, 1)
                        .ge(Book::getCreatedAt, startTime)
                        .le(Book::getCreatedAt, endTime)
        ));
        long bookTotal = nullToZero(bookMapper.selectCount(
                new LambdaQueryWrapper<Book>()
                        .ge(Book::getCreatedAt, startTime)
                        .le(Book::getCreatedAt, endTime)
        ));

        Map<String, Object> bookStats = new LinkedHashMap<>();
        bookStats.put("totalUploaded", bookTotal);
        bookStats.put("copyrightDeclared", bookWithCopyright);
        bookStats.put("takenDown", bookTakenDown);
        bookStats.put("copyrightRate", bookTotal > 0 ? String.format("%.1f%%", bookWithCopyright * 100.0 / bookTotal) : "N/A");
        report.put("bookCompliance", bookStats);

        LambdaQueryWrapper<OperationLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.ge(OperationLog::getCreatedAt, startTime);
        logWrapper.le(OperationLog::getCreatedAt, endTime);
        long operationCount = nullToZero(operationLogMapper.selectCount(logWrapper));

        Map<String, Object> operationStats = new LinkedHashMap<>();
        operationStats.put("totalOperations", operationCount);
        report.put("operationAudit", operationStats);

        report.put("generatedAt", LocalDateTime.now().toString());

        return report;
    }

    private long nullToZero(Long value) {
        return value != null ? value : 0L;
    }
}
