package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.dto.AiChatDTO;
import com.xiaoan.bookstore.dto.AiSummaryDTO;
import com.xiaoan.bookstore.entity.AiChatHistory;
import com.xiaoan.bookstore.entity.AiSummary;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.AiChatHistoryMapper;
import com.xiaoan.bookstore.mapper.AiSummaryMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final String DISCLAIMER = "【AI结果仅供参考，请以书籍原文为准】";
    private static final String COPYRIGHT_REMINDER = "请确保您拥有该书籍的合法版权，仅限个人学习使用。";

    private final AiSummaryMapper aiSummaryMapper;
    private final AiChatHistoryMapper aiChatHistoryMapper;
    private final BookService bookService;
    private final MembershipService membershipService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String generateSessionId(Long userId, Long bookId) {
        return "book_" + userId + "_" + bookId;
    }

    private void checkCopyrightAndQuota(Long userId, Long bookId) {
        membershipService.checkAiQuota(userId);

        Book book = bookService.detail(userId, bookId);
        if (book.getCopyrightDeclared() == null || book.getCopyrightDeclared() != 1) {
            throw new BusinessException("请先确认您拥有该书籍的合法版权");
        }
        if (book.getCopyrightAgreedAt() == null) {
            throw new BusinessException("请先确认版权声明后再使用AI功能");
        }
    }

    private AiChatHistory createChatHistory(Long userId, AiChatDTO dto, Book book) {
        AiChatHistory history = new AiChatHistory();
        history.setUserId(userId);
        history.setBookId(dto.getBookId());
        history.setBookTitle(book.getTitle());
        history.setSessionId(generateSessionId(userId, dto.getBookId()));
        history.setType(dto.getType());
        history.setSourceType(dto.getSourceType());
        history.setSourceText(dto.getSourceText());
        history.setPageNum(dto.getPageNum());
        history.setUserPrompt(dto.getUserPrompt());
        history.setStatus(AiChatHistory.Status.GENERATING);
        aiChatHistoryMapper.insert(history);
        return history;
    }

    private void updateChatHistory(AiChatHistory history, String response, String extraData) {
        history.setAiResponse(DISCLAIMER + "\n\n" + response);
        history.setExtraData(extraData);
        history.setStatus(AiChatHistory.Status.SUCCESS);
        aiChatHistoryMapper.updateById(history);
        membershipService.incrementAiUsage(history.getUserId());
    }

    private void handleChatError(AiChatHistory history, Exception e) {
        history.setStatus(AiChatHistory.Status.FAILED);
        history.setErrorMsg(e.getMessage());
        aiChatHistoryMapper.updateById(history);
        throw new BusinessException("AI处理失败: " + e.getMessage());
    }

    @Transactional
    public AiChatHistory processChat(Long userId, AiChatDTO dto) {
        checkCopyrightAndQuota(userId, dto.getBookId());

        Book book = bookService.detail(userId, dto.getBookId());
        AiChatHistory history = createChatHistory(userId, dto, book);

        try {
            String response;
            String extraData = null;

            switch (dto.getType()) {
                case AiChatHistory.Type.SUMMARY:
                    response = generateSummary(dto.getSourceText(), dto.getSourceType(), book.getTitle());
                    break;
                case AiChatHistory.Type.EXPLAIN:
                    response = generateExplanation(dto.getSourceText(), dto.getUserPrompt());
                    break;
                case AiChatHistory.Type.TRANSLATE:
                    response = generateTranslation(dto.getSourceText(), dto.getTargetLanguage());
                    break;
                case AiChatHistory.Type.QUIZ:
                    Map<String, Object> quizResult = generateQuiz(dto.getSourceText(), dto.getSourceType());
                    response = (String) quizResult.get("content");
                    extraData = objectMapper.writeValueAsString(quizResult.get("questions"));
                    break;
                case AiChatHistory.Type.OUTLINE:
                    response = generateOutline(book);
                    extraData = objectMapper.writeValueAsString(parseOutline(response));
                    break;
                case AiChatHistory.Type.KNOWLEDGE_CARD:
                    Map<String, Object> cardResult = generateKnowledgeCard(book);
                    response = (String) cardResult.get("content");
                    extraData = objectMapper.writeValueAsString(cardResult.get("card"));
                    break;
                default:
                    throw new BusinessException("不支持的AI操作类型");
            }

            updateChatHistory(history, response, extraData);
            log.info("AI处理成功: userId={}, bookId={}, type={}", userId, dto.getBookId(), dto.getType());
            return history;
        } catch (Exception e) {
            log.error("AI处理失败: userId={}, bookId={}, type={}", userId, dto.getBookId(), dto.getType(), e);
            handleChatError(history, e);
            return null;
        }
    }

    private String generateSummary(String text, Integer sourceType, String bookTitle) {
        String sourceDesc = switch (sourceType) {
            case AiChatHistory.SourceType.SELECTED_TEXT -> "选中段落";
            case AiChatHistory.SourceType.CURRENT_PAGE -> "当前页内容";
            default -> "全书内容";
        };

        if (sourceType == AiChatHistory.SourceType.WHOLE_BOOK) {
            return "《" + bookTitle + "》全书摘要：\n\n" +
                    "本书系统地阐述了核心主题，从基础概念到深入应用，全面覆盖了相关领域的知识体系。" +
                    "通过阅读本书，读者可以建立完整的知识框架，理解核心原理，并掌握实践方法。\n\n" +
                    "主要内容包括：\n" +
                    "• 基础理论与核心概念\n" +
                    "• 关键技术与方法论\n" +
                    "• 典型案例与实战应用\n" +
                    "• 进阶技巧与最佳实践\n" +
                    "• 未来发展趋势展望";
        }

        return sourceDesc + "摘要：\n\n" +
                "本段内容主要讨论了相关主题的核心要点，通过清晰的逻辑结构阐述了关键概念。" +
                "内容重点突出，层次分明，有助于读者快速理解和掌握核心思想。\n\n" +
                "核心要点：\n" +
                "• 主题的核心概念和定义\n" +
                "• 关键论点和论证过程\n" +
                "• 实践应用的指导意义\n" +
                "• 与其他知识点的关联";
    }

    private String generateExplanation(String text, String userPrompt) {
        String prompt = userPrompt != null && !userPrompt.isBlank() ? userPrompt : "请详细解释以下内容";

        return prompt + "：\n\n" +
                "【详细解释】\n" +
                "这段内容的核心含义是：通过系统的论述，阐明了主题的本质特征和内在规律。\n\n" +
                "【背景知识】\n" +
                "要理解这段内容，需要具备相关领域的基础知识，包括基本概念、理论框架等。" +
                "建议先了解该领域的入门知识，再深入学习本节内容。\n\n" +
                "【关键术语解析】\n" +
                "• 术语1：指的是...\n" +
                "• 术语2：表示...\n\n" +
                "【延伸阅读】\n" +
                "如果想深入了解，可以查阅相关章节或参考资料，进一步拓展知识面。";
    }

    private String generateTranslation(String text, String targetLanguage) {
        String lang = targetLanguage != null && !targetLanguage.isBlank() ? targetLanguage : "英文";

        return "【" + lang + "翻译】\n\n" +
                "This is the translated content of the selected text. The translation maintains the original meaning " +
                "while ensuring natural expression in the target language.\n\n" +
                "【翻译说明】\n" +
                "• 专业术语采用标准译法\n" +
                "• 长句进行了适当拆分，更符合目标语言表达习惯\n" +
                "• 文化背景相关内容进行了本地化处理";
    }

    private Map<String, Object> generateQuiz(String text, Integer sourceType) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> questions = new ArrayList<>();

        StringBuilder content = new StringBuilder("【自测题目】\n\n");

        String[] qTypes = {"单选题", "多选题", "判断题"};
        for (int i = 0; i < 3; i++) {
            Map<String, Object> q = new HashMap<>();
            q.put("id", i + 1);
            q.put("type", qTypes[i]);

            if (i == 0) {
                q.put("question", "根据内容，以下哪个选项是正确的？");
                q.put("options", Arrays.asList("A. 选项A描述", "B. 选项B描述", "C. 选项C描述", "D. 选项D描述"));
                q.put("answer", "B");
                q.put("explanation", "选项B正确，因为...");
            } else if (i == 1) {
                q.put("question", "以下哪些是本段提到的核心要点？（多选）");
                q.put("options", Arrays.asList("A. 要点一", "B. 要点二", "C. 要点三", "D. 要点四"));
                q.put("answer", "ABC");
                q.put("explanation", "A、B、C都是原文提到的要点，D未提及。");
            } else {
                q.put("question", "本段内容的核心观点是否正确？");
                q.put("options", Arrays.asList("A. 正确", "B. 错误"));
                q.put("answer", "A");
                q.put("explanation", "根据原文内容，该表述是正确的。");
            }
            questions.add(q);

            content.append(i + 1).append(". [").append(qTypes[i]).append("] ")
                    .append(q.get("question")).append("\n");
            for (String opt : (List<String>) q.get("options")) {
                content.append("   ").append(opt).append("\n");
            }
            content.append("\n");
        }

        content.append("【答案解析】\n");
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            content.append(i + 1).append(". 答案：").append(q.get("answer"))
                    .append("。").append(q.get("explanation")).append("\n");
        }

        result.put("content", content.toString());
        result.put("questions", questions);
        return result;
    }

    private String generateOutline(Book book) {
        return "《" + book.getTitle() + "》章节大纲：\n\n" +
                "【全书结构】\n" +
                "本书共分为三大部分，由浅入深地展开论述，逻辑清晰，层次分明。\n\n" +
                "第一部分：基础篇\n" +
                "• 第1章：入门概述 - 介绍基本概念和背景\n" +
                "• 第2章：核心原理 - 阐述理论基础\n" +
                "• 第3章：基础知识 - 讲解必备知识点\n\n" +
                "第二部分：进阶篇\n" +
                "• 第4章：深入分析 - 探讨深层机制\n" +
                "• 第5章：方法论 - 介绍实践方法\n" +
                "• 第6章：典型案例 - 分析实际应用\n\n" +
                "第三部分：实战篇\n" +
                "• 第7章：综合应用 - 整合知识解决问题\n" +
                "• 第8章：最佳实践 - 总结经验技巧\n" +
                "• 第9章：未来展望 - 探讨发展趋势\n\n" +
                "【阅读建议】\n" +
                "建议按照章节顺序阅读，每章结束后完成课后练习，巩固所学知识。";
    }

    private List<Map<String, Object>> parseOutline(String outline) {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] lines = outline.split("\n");
        String currentPart = "";

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("第") && line.contains("部分：")) {
                currentPart = line.substring(0, line.indexOf("：") + 1);
            } else if (line.startsWith("• 第") && line.contains("章：")) {
                Map<String, Object> chapter = new HashMap<>();
                chapter.put("part", currentPart);
                int chapterEnd = line.indexOf("：");
                chapter.put("chapter", line.substring(2, chapterEnd));
                chapter.put("title", line.substring(chapterEnd + 1).trim());
                result.add(chapter);
            }
        }
        return result;
    }

    private Map<String, Object> generateKnowledgeCard(Book book) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> card = new LinkedHashMap<>();

        card.put("书名", book.getTitle());
        card.put("作者", book.getAuthor() != null ? book.getAuthor() : "未知");
        card.put("核心主题", "本书围绕核心主题展开系统论述");
        card.put("关键概念", Arrays.asList("概念1", "概念2", "概念3"));
        card.put("核心理论", "阐述了...的核心理论框架");
        card.put("实践方法", "提供了...的实践方法论");
        card.put("典型应用", Arrays.asList("场景1", "场景2", "场景3"));
        card.put("阅读收获", Arrays.asList(
                "建立完整的知识体系",
                "掌握核心技能",
                "提升解决问题的能力"
        ));
        card.put("推荐指数", "★★★★★");

        StringBuilder content = new StringBuilder("【知识卡片】\n\n");
        content.append("📚 书名：").append(card.get("书名")).append("\n");
        content.append("✍️ 作者：").append(card.get("作者")).append("\n");
        content.append("🎯 核心主题：").append(card.get("核心主题")).append("\n");
        content.append("\n📌 关键概念：\n");
        for (String concept : (List<String>) card.get("关键概念")) {
            content.append("   • ").append(concept).append("\n");
        }
        content.append("\n💡 核心理论：\n   ").append(card.get("核心理论")).append("\n");
        content.append("\n🔧 实践方法：\n   ").append(card.get("实践方法")).append("\n");
        content.append("\n🌍 典型应用：\n");
        for (String app : (List<String>) card.get("典型应用")) {
            content.append("   • ").append(app).append("\n");
        }
        content.append("\n🎓 阅读收获：\n");
        for (String gain : (List<String>) card.get("阅读收获")) {
            content.append("   • ").append(gain).append("\n");
        }
        content.append("\n⭐ 推荐指数：").append(card.get("推荐指数"));

        result.put("content", content.toString());
        result.put("card", card);
        return result;
    }

    public Page<AiChatHistory> getChatHistoryByBook(Long userId, Long bookId, int page, int size) {
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatHistory::getUserId, userId)
                .eq(AiChatHistory::getBookId, bookId)
                .eq(AiChatHistory::getIsDeleted, 0)
                .orderByDesc(AiChatHistory::getCreatedAt);
        return aiChatHistoryMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public List<Map<String, Object>> getBookChatSessions(Long userId) {
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatHistory::getUserId, userId)
                .eq(AiChatHistory::getIsDeleted, 0)
                .groupBy(AiChatHistory::getBookId)
                .select(AiChatHistory::getBookId, AiChatHistory::getBookTitle,
                        AiChatHistory::getSessionId, AiChatHistory::getCreatedAt)
                .orderByDesc(AiChatHistory::getCreatedAt);

        List<AiChatHistory> list = aiChatHistoryMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();

        for (AiChatHistory item : list) {
            LambdaQueryWrapper<AiChatHistory> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(AiChatHistory::getUserId, userId)
                    .eq(AiChatHistory::getBookId, item.getBookId())
                    .eq(AiChatHistory::getIsDeleted, 0);
            Long count = aiChatHistoryMapper.selectCount(countWrapper);

            Map<String, Object> session = new HashMap<>();
            session.put("bookId", item.getBookId());
            session.put("bookTitle", item.getBookTitle());
            session.put("sessionId", item.getSessionId());
            session.put("chatCount", count);
            session.put("lastActive", item.getCreatedAt());
            result.add(session);
        }
        return result;
    }

    public AiChatHistory getChatDetail(Long userId, Long id) {
        AiChatHistory history = aiChatHistoryMapper.selectById(id);
        if (history == null) {
            return null;
        }
        TenantValidator.validateCrossTenant(history.getUserId(), userId);
        return history;
    }

    @Transactional
    public void deleteChat(Long userId, Long id) {
        AiChatHistory history = aiChatHistoryMapper.selectById(id);
        if (history == null) {
            throw new BusinessException("记录不存在");
        }
        TenantValidator.validateCrossTenant(history.getUserId(), userId);
        aiChatHistoryMapper.deleteById(id);
        log.info("删除AI对话记录: userId={}, id={}", userId, id);
    }

    @Transactional
    public void clearBookChats(Long userId, Long bookId) {
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatHistory::getUserId, userId)
                .eq(AiChatHistory::getBookId, bookId);
        aiChatHistoryMapper.delete(wrapper);
        log.info("清空书籍AI对话记录: userId={}, bookId={}", userId, bookId);
    }

    public Map<String, Object> getDisclaimerInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("disclaimer", DISCLAIMER);
        info.put("copyrightReminder", COPYRIGHT_REMINDER);
        info.put("version", "1.0");
        return info;
    }

    @Transactional
    public void agreeCopyright(Long userId, Long bookId) {
        Book book = bookService.detail(userId, bookId);
        TenantValidator.validateCrossTenant(book.getUserId(), userId);
        book.setCopyrightDeclared(1);
        book.setCopyrightAgreedAt(LocalDateTime.now());
        bookService.update(book);
        log.info("用户确认书籍版权: userId={}, bookId={}, bookTitle={}", userId, bookId, book.getTitle());
    }

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
