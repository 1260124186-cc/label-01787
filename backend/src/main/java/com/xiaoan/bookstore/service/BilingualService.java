package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.dto.BilingualAlignmentVO;
import com.xiaoan.bookstore.dto.BilingualPairCreateDTO;
import com.xiaoan.bookstore.dto.BilingualPairUpdateDTO;
import com.xiaoan.bookstore.dto.BilingualPairVO;
import com.xiaoan.bookstore.entity.BilingualAlignment;
import com.xiaoan.bookstore.entity.BilingualPair;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.BilingualAlignmentMapper;
import com.xiaoan.bookstore.mapper.BilingualPairMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BilingualService {

    private static final Logger log = LoggerFactory.getLogger(BilingualService.class);

    public static final int ALIGNMENT_STRATEGY_CHAPTER = 1;
    public static final int ALIGNMENT_STRATEGY_AI = 2;

    public static final int ALIGNMENT_METHOD_CHAPTER = 1;
    public static final int ALIGNMENT_METHOD_AI = 2;
    public static final int ALIGNMENT_METHOD_MANUAL = 3;

    public static final int AI_STATUS_IDLE = 0;
    public static final int AI_STATUS_RUNNING = 1;
    public static final int AI_STATUS_DONE = 2;
    public static final int AI_STATUS_FAILED = 3;

    public static final int UNIT_TYPE_PAGE = 1;
    public static final int UNIT_TYPE_CHAPTER = 2;

    private final BilingualPairMapper pairMapper;
    private final BilingualAlignmentMapper alignmentMapper;
    private final BookService bookService;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Transactional
    public BilingualPair createPair(Long userId, BilingualPairCreateDTO dto) {
        if (dto.getLeftBookId().equals(dto.getRightBookId())) {
            throw new BusinessException("不能选择同一本书作为双语对照");
        }

        Book leftBook = bookService.getBookById(userId, dto.getLeftBookId());
        Book rightBook = bookService.getBookById(userId, dto.getRightBookId());

        LambdaQueryWrapper<BilingualPair> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(BilingualPair::getUserId, userId)
                .eq(BilingualPair::getStatus, Constants.STATUS_ENABLED)
                .and(w -> w.and(ww -> ww.eq(BilingualPair::getLeftBookId, dto.getLeftBookId())
                                .eq(BilingualPair::getRightBookId, dto.getRightBookId()))
                        .or(ww -> ww.eq(BilingualPair::getLeftBookId, dto.getRightBookId())
                                .eq(BilingualPair::getRightBookId, dto.getLeftBookId())));
        if (pairMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException("这两本书已存在双语关联");
        }

        BilingualPair pair = new BilingualPair();
        pair.setUserId(userId);
        pair.setLeftBookId(dto.getLeftBookId());
        pair.setRightBookId(dto.getRightBookId());
        pair.setLeftLanguage(dto.getLeftLanguage() != null ? dto.getLeftLanguage() : "");
        pair.setRightLanguage(dto.getRightLanguage() != null ? dto.getRightLanguage() : "");
        pair.setAlignmentStrategy(dto.getAlignmentStrategy() != null ? dto.getAlignmentStrategy() : ALIGNMENT_STRATEGY_CHAPTER);
        pair.setName(dto.getName() != null && !dto.getName().isBlank()
                ? dto.getName()
                : leftBook.getTitle() + " ↔ " + rightBook.getTitle());
        pair.setLastLeftUnit(0);
        pair.setLastRightUnit(0);
        pair.setLeftUnitType(Constants.FORMAT_EPUB.equals(leftBook.getBookFormat()) ? UNIT_TYPE_CHAPTER : UNIT_TYPE_PAGE);
        pair.setRightUnitType(Constants.FORMAT_EPUB.equals(rightBook.getBookFormat()) ? UNIT_TYPE_CHAPTER : UNIT_TYPE_PAGE);
        pair.setSyncEnabled(1);
        pair.setAiAlignmentStatus(AI_STATUS_IDLE);
        pair.setAiAlignmentProgress(0);
        pair.setAiAlignmentError("");
        pair.setStatus(Constants.STATUS_ENABLED);
        pair.setCreatedAt(LocalDateTime.now());
        pair.setUpdatedAt(LocalDateTime.now());
        pairMapper.insert(pair);

        if (pair.getAlignmentStrategy() == ALIGNMENT_STRATEGY_CHAPTER) {
            generateChapterAlignments(pair);
        }

        log.info("创建双语关联成功: userId={}, pairId={}, left={}, right={}",
                userId, pair.getId(), leftBook.getTitle(), rightBook.getTitle());
        return pair;
    }

    public Page<BilingualPairVO> listPairs(Long userId, int page, int size) {
        LambdaQueryWrapper<BilingualPair> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BilingualPair::getUserId, userId)
                .eq(BilingualPair::getStatus, Constants.STATUS_ENABLED)
                .orderByDesc(BilingualPair::getUpdatedAt);
        Page<BilingualPair> p = pairMapper.selectPage(new Page<>(page, size), wrapper);

        Page<BilingualPairVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        List<BilingualPairVO> records = new ArrayList<>();
        for (BilingualPair pair : p.getRecords()) {
            records.add(buildPairVO(pair));
        }
        result.setRecords(records);
        return result;
    }

    public BilingualPairVO getPairDetail(Long userId, Long pairId) {
        BilingualPair pair = getPairInternal(userId, pairId);
        return buildPairVO(pair);
    }

    public List<BilingualPairVO> getPairsByBook(Long userId, Long bookId) {
        LambdaQueryWrapper<BilingualPair> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BilingualPair::getUserId, userId)
                .eq(BilingualPair::getStatus, Constants.STATUS_ENABLED)
                .and(w -> w.eq(BilingualPair::getLeftBookId, bookId)
                        .or().eq(BilingualPair::getRightBookId, bookId))
                .orderByDesc(BilingualPair::getUpdatedAt);
        List<BilingualPair> pairs = pairMapper.selectList(wrapper);
        List<BilingualPairVO> result = new ArrayList<>();
        for (BilingualPair pair : pairs) {
            result.add(buildPairVO(pair));
        }
        return result;
    }

    @Transactional
    public BilingualPair updatePair(Long userId, Long pairId, BilingualPairUpdateDTO dto) {
        BilingualPair pair = getPairInternal(userId, pairId);
        boolean changed = false;

        if (dto.getLeftLanguage() != null && !dto.getLeftLanguage().equals(pair.getLeftLanguage())) {
            pair.setLeftLanguage(dto.getLeftLanguage());
            changed = true;
        }
        if (dto.getRightLanguage() != null && !dto.getRightLanguage().equals(pair.getRightLanguage())) {
            pair.setRightLanguage(dto.getRightLanguage());
            changed = true;
        }
        if (dto.getAlignmentStrategy() != null && !dto.getAlignmentStrategy().equals(pair.getAlignmentStrategy())) {
            pair.setAlignmentStrategy(dto.getAlignmentStrategy());
            changed = true;
            if (dto.getAlignmentStrategy() == ALIGNMENT_STRATEGY_CHAPTER) {
                LambdaQueryWrapper<BilingualAlignment> delWrapper = new LambdaQueryWrapper<>();
                delWrapper.eq(BilingualAlignment::getPairId, pairId);
                alignmentMapper.delete(delWrapper);
                generateChapterAlignments(pair);
                pair.setAiAlignmentStatus(AI_STATUS_IDLE);
                pair.setAiAlignmentProgress(0);
            }
        }
        if (dto.getName() != null && !dto.getName().isBlank() && !dto.getName().equals(pair.getName())) {
            pair.setName(dto.getName());
            changed = true;
        }
        if (dto.getSyncEnabled() != null && !dto.getSyncEnabled().equals(pair.getSyncEnabled())) {
            pair.setSyncEnabled(dto.getSyncEnabled());
            changed = true;
        }
        if (dto.getLastLeftUnit() != null && !dto.getLastLeftUnit().equals(pair.getLastLeftUnit())) {
            pair.setLastLeftUnit(dto.getLastLeftUnit());
            changed = true;
        }
        if (dto.getLastRightUnit() != null && !dto.getLastRightUnit().equals(pair.getLastRightUnit())) {
            pair.setLastRightUnit(dto.getLastRightUnit());
            changed = true;
        }

        if (changed) {
            pair.setUpdatedAt(LocalDateTime.now());
            pairMapper.updateById(pair);
            log.info("更新双语关联: pairId={}", pairId);
        }
        return pair;
    }

    @Transactional
    public void deletePair(Long userId, Long pairId) {
        BilingualPair pair = getPairInternal(userId, pairId);
        pair.setStatus(Constants.STATUS_DISABLED);
        pair.setUpdatedAt(LocalDateTime.now());
        pairMapper.updateById(pair);
        log.info("删除双语关联: userId={}, pairId={}", userId, pairId);
    }

    private void generateChapterAlignments(BilingualPair pair) {
        try {
            Book leftBook = bookService.getBookById(pair.getUserId(), pair.getLeftBookId());
            Book rightBook = bookService.getBookById(pair.getUserId(), pair.getRightBookId());

            int leftTotal = pair.getLeftUnitType() == UNIT_TYPE_CHAPTER
                    ? (leftBook.getChapterCount() != null ? leftBook.getChapterCount() : 0)
                    : (leftBook.getPageCount() != null ? leftBook.getPageCount() : 0);
            int rightTotal = pair.getRightUnitType() == UNIT_TYPE_CHAPTER
                    ? (rightBook.getChapterCount() != null ? rightBook.getChapterCount() : 0)
                    : (rightBook.getPageCount() != null ? rightBook.getPageCount() : 0);

            int maxUnits = Math.max(leftTotal, rightTotal);
            if (maxUnits <= 0) return;

            List<BilingualAlignment> alignments = new ArrayList<>();
            for (int i = 0; i < maxUnits; i++) {
                int leftIdx = i < leftTotal ? i : Math.max(0, leftTotal - 1);
                int rightIdx = i < rightTotal ? i : Math.max(0, rightTotal - 1);

                BilingualAlignment a = new BilingualAlignment();
                a.setPairId(pair.getId());
                a.setLeftUnitIndex(leftIdx);
                a.setRightUnitIndex(rightIdx);
                a.setLeftParagraphIndex(0);
                a.setRightParagraphIndex(0);
                a.setAlignmentMethod(ALIGNMENT_METHOD_CHAPTER);
                a.setConfidence(new BigDecimal("1.0"));
                a.setSortOrder(i);
                a.setCreatedAt(LocalDateTime.now());
                a.setUpdatedAt(LocalDateTime.now());
                alignments.add(a);

                if (alignments.size() >= 100) {
                    for (BilingualAlignment item : alignments) {
                        alignmentMapper.insert(item);
                    }
                    alignments.clear();
                }
            }
            for (BilingualAlignment item : alignments) {
                alignmentMapper.insert(item);
            }
            log.info("章节号对齐完成: pairId={}, count={}", pair.getId(), maxUnits);
        } catch (Exception e) {
            log.error("生成章节对齐失败", e);
            throw new BusinessException("生成章节对齐失败: " + e.getMessage());
        }
    }

    public List<BilingualAlignmentVO> listAlignments(Long userId, Long pairId, Integer leftUnitIndex, Integer rightUnitIndex) {
        BilingualPair pair = getPairInternal(userId, pairId);
        LambdaQueryWrapper<BilingualAlignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BilingualAlignment::getPairId, pairId);
        if (leftUnitIndex != null) {
            wrapper.eq(BilingualAlignment::getLeftUnitIndex, leftUnitIndex);
        }
        if (rightUnitIndex != null) {
            wrapper.eq(BilingualAlignment::getRightUnitIndex, rightUnitIndex);
        }
        wrapper.orderByAsc(BilingualAlignment::getSortOrder, BilingualAlignment::getId);
        List<BilingualAlignment> list = alignmentMapper.selectList(wrapper);

        List<BilingualAlignmentVO> result = new ArrayList<>();
        for (BilingualAlignment a : list) {
            result.add(BilingualAlignmentVO.fromEntity(a));
        }
        return result;
    }

    public BilingualAlignmentVO findAlignedUnit(Long userId, Long pairId, String side, int unitIndex) {
        BilingualPair pair = getPairInternal(userId, pairId);
        LambdaQueryWrapper<BilingualAlignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BilingualAlignment::getPairId, pairId);
        if ("left".equals(side)) {
            wrapper.eq(BilingualAlignment::getLeftUnitIndex, unitIndex);
        } else {
            wrapper.eq(BilingualAlignment::getRightUnitIndex, unitIndex);
        }
        wrapper.orderByDesc(BilingualAlignment::getConfidence)
                .last("LIMIT 1");
        BilingualAlignment a = alignmentMapper.selectOne(wrapper);
        return a != null ? BilingualAlignmentVO.fromEntity(a) : null;
    }

    @Async
    @Transactional
    public void startAiAlignment(Long userId, Long pairId) {
        BilingualPair pair = getPairInternal(userId, pairId);
        if (pair.getAiAlignmentStatus() == AI_STATUS_RUNNING) {
            throw new BusinessException("AI对齐正在进行中");
        }

        pair.setAiAlignmentStatus(AI_STATUS_RUNNING);
        pair.setAiAlignmentProgress(0);
        pair.setAiAlignmentError("");
        pair.setUpdatedAt(LocalDateTime.now());
        pairMapper.updateById(pair);

        try {
            Book leftBook = bookService.getBookById(userId, pair.getLeftBookId());
            Book rightBook = bookService.getBookById(userId, pair.getRightBookId());

            int leftTotal = pair.getLeftUnitType() == UNIT_TYPE_CHAPTER
                    ? (leftBook.getChapterCount() != null ? leftBook.getChapterCount() : 0)
                    : (leftBook.getPageCount() != null ? leftBook.getPageCount() : 0);
            int rightTotal = pair.getRightUnitType() == UNIT_TYPE_CHAPTER
                    ? (rightBook.getChapterCount() != null ? rightBook.getChapterCount() : 0)
                    : (rightBook.getPageCount() != null ? rightBook.getPageCount() : 0);

            LambdaQueryWrapper<BilingualAlignment> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(BilingualAlignment::getPairId, pairId);
            alignmentMapper.delete(delWrapper);

            List<BilingualAlignment> alignments = new ArrayList<>();
            int leftSampleSize = Math.min(leftTotal, 50);
            int rightSampleSize = Math.min(rightTotal, 50);

            for (int i = 0; i < Math.max(leftSampleSize, rightSampleSize); i++) {
                int leftIdx = i < leftSampleSize ? i : Math.max(0, leftSampleSize - 1);
                int rightIdx = i < rightSampleSize ? i : Math.max(0, rightSampleSize - 1);

                String leftText = extractTextSnippet(userId, pair.getLeftBookId(), pair.getLeftUnitType(), leftIdx);
                String rightText = extractTextSnippet(userId, pair.getRightBookId(), pair.getRightUnitType(), rightIdx);

                BigDecimal confidence = calculateSimilarity(leftText, rightText);

                BilingualAlignment a = new BilingualAlignment();
                a.setPairId(pair.getId());
                a.setLeftUnitIndex(leftIdx);
                a.setRightUnitIndex(rightIdx);
                a.setLeftParagraphIndex(0);
                a.setRightParagraphIndex(0);
                a.setLeftTextHash(md5(leftText));
                a.setRightTextHash(md5(rightText));
                a.setAlignmentMethod(ALIGNMENT_METHOD_AI);
                a.setConfidence(confidence);
                a.setLeftTextSnippet(truncate(leftText, 200));
                a.setRightTextSnippet(truncate(rightText, 200));
                a.setSortOrder(i);
                a.setCreatedAt(LocalDateTime.now());
                a.setUpdatedAt(LocalDateTime.now());
                alignments.add(a);

                int progress = (int) ((i + 1) * 100.0 / Math.max(leftSampleSize, rightSampleSize));
                pair.setAiAlignmentProgress(progress);
                pairMapper.updateById(pair);
            }

            for (BilingualAlignment a : alignments) {
                alignmentMapper.insert(a);
            }

            int remainingLeft = Math.max(0, leftTotal - leftSampleSize);
            int remainingRight = Math.max(0, rightTotal - rightSampleSize);
            int maxRemaining = Math.max(remainingLeft, remainingRight);
            if (maxRemaining > 0) {
                List<BilingualAlignment> remaining = new ArrayList<>();
                int baseOrder = alignments.size();
                for (int i = 0; i < maxRemaining; i++) {
                    int lIdx = leftSampleSize + i;
                    int rIdx = rightSampleSize + i;
                    if (lIdx >= leftTotal) lIdx = leftTotal - 1;
                    if (rIdx >= rightTotal) rIdx = rightTotal - 1;

                    BilingualAlignment a = new BilingualAlignment();
                    a.setPairId(pair.getId());
                    a.setLeftUnitIndex(lIdx);
                    a.setRightUnitIndex(rIdx);
                    a.setAlignmentMethod(ALIGNMENT_METHOD_AI);
                    a.setConfidence(new BigDecimal("0.8"));
                    a.setSortOrder(baseOrder + i);
                    a.setCreatedAt(LocalDateTime.now());
                    a.setUpdatedAt(LocalDateTime.now());
                    remaining.add(a);
                }
                for (BilingualAlignment a : remaining) {
                    alignmentMapper.insert(a);
                }
            }

            pair.setAiAlignmentStatus(AI_STATUS_DONE);
            pair.setAiAlignmentProgress(100);
            pair.setUpdatedAt(LocalDateTime.now());
            pairMapper.updateById(pair);
            log.info("AI对齐完成: pairId={}", pairId);

        } catch (Exception e) {
            log.error("AI对齐失败", e);
            pair.setAiAlignmentStatus(AI_STATUS_FAILED);
            pair.setAiAlignmentError(e.getMessage() != null ? truncate(e.getMessage(), 500) : "未知错误");
            pair.setUpdatedAt(LocalDateTime.now());
            pairMapper.updateById(pair);
        }
    }

    @Transactional
    public BilingualAlignment addManualAlignment(Long userId, Long pairId, Integer leftUnitIndex, Integer rightUnitIndex,
                                                 Integer leftParagraphIndex, Integer rightParagraphIndex) {
        BilingualPair pair = getPairInternal(userId, pairId);

        BilingualAlignment a = new BilingualAlignment();
        a.setPairId(pairId);
        a.setLeftUnitIndex(leftUnitIndex);
        a.setRightUnitIndex(rightUnitIndex);
        a.setLeftParagraphIndex(leftParagraphIndex != null ? leftParagraphIndex : 0);
        a.setRightParagraphIndex(rightParagraphIndex != null ? rightParagraphIndex : 0);
        a.setAlignmentMethod(ALIGNMENT_METHOD_MANUAL);
        a.setConfidence(new BigDecimal("1.0"));

        LambdaQueryWrapper<BilingualAlignment> maxWrapper = new LambdaQueryWrapper<>();
        maxWrapper.eq(BilingualAlignment::getPairId, pairId);
        maxWrapper.orderByDesc(BilingualAlignment::getSortOrder);
        maxWrapper.last("LIMIT 1");
        BilingualAlignment last = alignmentMapper.selectOne(maxWrapper);
        a.setSortOrder(last != null && last.getSortOrder() != null ? last.getSortOrder() + 1 : 0);

        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        alignmentMapper.insert(a);
        return a;
    }

    @Transactional
    public void deleteAlignment(Long userId, Long pairId, Long alignmentId) {
        getPairInternal(userId, pairId);
        BilingualAlignment a = alignmentMapper.selectById(alignmentId);
        if (a == null || !a.getPairId().equals(pairId)) {
            throw new BusinessException("对齐记录不存在");
        }
        alignmentMapper.deleteById(alignmentId);
    }

    public Map<String, Object> getReadingProgress(Long userId, Long pairId) {
        BilingualPair pair = getPairInternal(userId, pairId);
        Book leftBook = bookService.getBookById(userId, pair.getLeftBookId());
        Book rightBook = bookService.getBookById(userId, pair.getRightBookId());

        int leftTotal = pair.getLeftUnitType() == UNIT_TYPE_CHAPTER
                ? (leftBook.getChapterCount() != null ? leftBook.getChapterCount() : 0)
                : (leftBook.getPageCount() != null ? leftBook.getPageCount() : 0);
        int rightTotal = pair.getRightUnitType() == UNIT_TYPE_CHAPTER
                ? (rightBook.getChapterCount() != null ? rightBook.getChapterCount() : 0)
                : (rightBook.getPageCount() != null ? rightBook.getPageCount() : 0);

        int lastLeft = pair.getLastLeftUnit() != null ? pair.getLastLeftUnit() : 0;
        int lastRight = pair.getLastRightUnit() != null ? pair.getLastRightUnit() : 0;

        int leftProgress = leftTotal > 0 ? Math.min(100, Math.round(lastLeft * 100.0f / leftTotal)) : 0;
        int rightProgress = rightTotal > 0 ? Math.min(100, Math.round(lastRight * 100.0f / rightTotal)) : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("leftUnit", lastLeft);
        result.put("rightUnit", lastRight);
        result.put("leftTotal", leftTotal);
        result.put("rightTotal", rightTotal);
        result.put("leftProgress", leftProgress);
        result.put("rightProgress", rightProgress);
        result.put("syncEnabled", pair.getSyncEnabled());
        result.put("alignmentStrategy", pair.getAlignmentStrategy());
        result.put("aiAlignmentStatus", pair.getAiAlignmentStatus());
        result.put("aiAlignmentProgress", pair.getAiAlignmentProgress());
        return result;
    }

    private BilingualPair getPairInternal(Long userId, Long pairId) {
        BilingualPair pair = pairMapper.selectById(pairId);
        if (pair == null || pair.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException("双语关联不存在");
        }
        TenantValidator.validateCrossTenant(pair.getUserId(), userId);
        return pair;
    }

    private BilingualPairVO buildPairVO(BilingualPair pair) {
        BilingualPairVO vo = BilingualPairVO.fromEntity(pair);
        try {
            Book leftBook = bookService.getBookById(pair.getUserId(), pair.getLeftBookId());
            Book rightBook = bookService.getBookById(pair.getUserId(), pair.getRightBookId());
            vo.setLeftBookTitle(leftBook.getTitle());
            vo.setRightBookTitle(rightBook.getTitle());
            vo.setLeftBookFormat(leftBook.getBookFormat());
            vo.setRightBookFormat(rightBook.getBookFormat());
        } catch (Exception e) {
            log.warn("加载关联书籍信息失败: pairId={}", pair.getId(), e);
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (pair.getCreatedAt() != null) vo.setCreatedAt(pair.getCreatedAt().format(dtf));
        if (pair.getUpdatedAt() != null) vo.setUpdatedAt(pair.getUpdatedAt().format(dtf));
        return vo;
    }

    private String extractTextSnippet(Long userId, Long bookId, int unitType, int unitIndex) {
        try {
            String content;
            if (unitType == UNIT_TYPE_CHAPTER) {
                content = bookService.getChapterHtml(userId, bookId, unitIndex);
            } else {
                content = bookService.getPageText(userId, bookId, unitIndex + 1);
            }
            if (content == null) return "";
            String text = content.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
            return text;
        } catch (Exception e) {
            log.warn("提取文本片段失败 bookId={}, unit={}", bookId, unitIndex, e);
            return "";
        }
    }

    private BigDecimal calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isBlank() || text2.isBlank()) {
            return new BigDecimal("0.5");
        }
        String s1 = normalize(text1);
        String s2 = normalize(text2);
        if (s1.isEmpty() || s2.isEmpty()) return new BigDecimal("0.5");

        int matchCount = 0;
        Pattern chapterPattern = Pattern.compile("(chapter|第[一二三四五六七八九十百千0-9]+[章节篇部卷])", Pattern.CASE_INSENSITIVE);
        Matcher m1 = chapterPattern.matcher(s1);
        Matcher m2 = chapterPattern.matcher(s2);
        Set<String> markers1 = new HashSet<>();
        Set<String> markers2 = new HashSet<>();
        while (m1.find()) markers1.add(m1.group().toLowerCase());
        while (m2.find()) markers2.add(m2.group().toLowerCase());
        for (String m : markers1) {
            if (markers2.contains(m)) matchCount += 5;
        }

        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");
        int minWords = Math.min(words1.length, words2.length);
        int wordMatches = 0;
        Set<String> wordSet2 = new HashSet<>(Arrays.asList(words2));
        for (String w : words1) {
            if (w.length() > 2 && wordSet2.contains(w)) wordMatches++;
        }
        matchCount += wordMatches;

        int totalChecks = Math.max(1, markers1.size() + markers2.size() + minWords);
        double score = Math.min(1.0, matchCount * 2.0 / totalChecks);
        score = Math.max(0.3, score);
        return new BigDecimal(String.format("%.4f", score));
    }

    private String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[\\p{Punct}，。！？、；：\"\"''（）【】《》]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String md5(String s) {
        if (s == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
