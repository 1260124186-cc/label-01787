package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.dto.AiSummaryDTO;
import com.xiaoan.bookstore.entity.AiSummary;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.AiSummaryMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private final AiSummaryMapper aiSummaryMapper;
    private final BookService bookService;
    private final MembershipService membershipService;

    public AiSummary generateSummary(Long userId, AiSummaryDTO dto) {
        membershipService.checkAiQuota(userId);

        Book book = bookService.detail(userId, dto.getBookId());

        LambdaQueryWrapper<AiSummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiSummary::getUserId, userId)
                .eq(AiSummary::getBookId, dto.getBookId())
                .eq(AiSummary::getStatus, 1)
                .orderByDesc(AiSummary::getCreatedAt)
                .last("LIMIT 1");
        AiSummary existing = aiSummaryMapper.selectOne(wrapper);
        if (existing != null) {
            return existing;
        }

        AiSummary summary = new AiSummary();
        summary.setUserId(userId);
        summary.setBookId(dto.getBookId());
        summary.setBookTitle(book.getTitle());
        summary.setStatus(2);
        aiSummaryMapper.insert(summary);

        try {
            String generatedSummary = simulateAiSummary(book.getTitle());
            String keyPoints = simulateKeyPoints(book.getTitle());

            summary.setSummary(generatedSummary);
            summary.setKeyPoints(keyPoints);
            summary.setStatus(1);
            aiSummaryMapper.updateById(summary);

            membershipService.incrementAiUsage(userId);

            log.info("AI摘要生成成功: userId={}, bookId={}, bookTitle={}", userId, dto.getBookId(), book.getTitle());
        } catch (Exception e) {
            summary.setStatus(0);
            summary.setErrorMsg(e.getMessage());
            aiSummaryMapper.updateById(summary);
            throw new BusinessException("AI摘要生成失败: " + e.getMessage());
        }

        return summary;
    }

    public AiSummary getLatestSummary(Long userId, Long bookId) {
        LambdaQueryWrapper<AiSummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiSummary::getUserId, userId)
                .eq(AiSummary::getBookId, bookId)
                .orderByDesc(AiSummary::getCreatedAt)
                .last("LIMIT 1");
        AiSummary summary = aiSummaryMapper.selectOne(wrapper);
        if (summary == null) {
            return null;
        }
        TenantValidator.validateCrossTenant(summary.getUserId(), TenantContext.getTenantId());
        return summary;
    }

    public Page<AiSummary> mySummaries(Long userId, int page, int size) {
        LambdaQueryWrapper<AiSummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiSummary::getUserId, userId)
                .eq(AiSummary::getStatus, 1)
                .orderByDesc(AiSummary::getCreatedAt);
        return aiSummaryMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<AiSummary> adminList(int page, int size, Long userId) {
        LambdaQueryWrapper<AiSummary> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(AiSummary::getUserId, userId);
        }
        wrapper.orderByDesc(AiSummary::getCreatedAt);
        return aiSummaryMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public AiSummary getById(Long id) {
        return aiSummaryMapper.selectById(id);
    }

    private String simulateAiSummary(String bookTitle) {
        return "《" + bookTitle + "》是一本引人入胜的著作。本书从核心概念出发，系统地阐述了主题的各个方面。" +
                "作者以独到的视角和深入的分析，引导读者逐步理解复杂的内容。全书结构清晰，逻辑严密，" +
                "既有理论高度，又有实践指导意义。通过阅读本书，读者将获得对该领域的全面认知，" +
                "并能够将所学知识应用到实际场景中。";
    }

    private String simulateKeyPoints(String bookTitle) {
        List<String> points = Arrays.asList(
                "核心概念与基础理论",
                "核心方法论与实践框架",
                "典型案例深度解析",
                "进阶技巧与最佳实践",
                "未来发展趋势展望"
        );
        return String.join("\n", points.stream()
                .map(p -> "• " + p)
                .collect(Collectors.toList()));
    }
}
