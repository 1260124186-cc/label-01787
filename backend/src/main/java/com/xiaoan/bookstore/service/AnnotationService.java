package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.dto.AnnotationDTO;
import com.xiaoan.bookstore.entity.Annotation;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.AnnotationMapper;
import com.xiaoan.bookstore.mapper.BookMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnotationService {

    private static final Logger log = LoggerFactory.getLogger(AnnotationService.class);
    private final AnnotationMapper annotationMapper;
    private final ContentComplianceService contentComplianceService;
    private final PointsService pointsService;
    private final BookMapper bookMapper;

    public Annotation create(Long userId, AnnotationDTO dto) {
        Annotation ann = new Annotation();
        ann.setUserId(userId);
        ann.setBookId(dto.getBookId());
        ann.setPageNum(dto.getPageNum());
        ann.setSelectedText(dto.getSelectedText());
        ann.setContent(dto.getContent());
        ann.setType(dto.getType());
        ann.setTags(dto.getTags() != null ? dto.getTags() : "");
        ann.setIsPinned(dto.getIsPinned() != null ? dto.getIsPinned() : 0);
        ann.setColor(dto.getColor() != null ? dto.getColor() : Constants.ANNOTATION_COLOR_YELLOW);
        annotationMapper.insert(ann);

        try {
            String auditContent = (dto.getContent() != null ? dto.getContent() : "");
            if (dto.getSelectedText() != null && !dto.getSelectedText().isEmpty()) {
                auditContent = dto.getSelectedText() + " " + auditContent;
            }
            contentComplianceService.auditText(Constants.AUDIT_TARGET_ANNOTATION, ann.getId(), auditContent);
        } catch (Exception e) {
            log.warn("批注内容审核失败，不影响保存: {}", e.getMessage());
        }

        if (dto.getType() != null && dto.getType() == Constants.ANNOTATION_NOTE && dto.getSelectedText() != null && !dto.getSelectedText().isEmpty()) {
            try {
                pointsService.earnPoints(userId, Constants.POINTS_CATEGORY_SHARE_EXCERPT, 0, "分享书摘", String.valueOf(ann.getId()));
            } catch (Exception e) {
                log.warn("分享书摘积分发放失败，不影响保存: {}", e.getMessage());
            }
        }

        log.info("添加批注: userId={}, bookId={}, page={}", userId, dto.getBookId(), dto.getPageNum());
        return ann;
    }

    public Page<Annotation> list(Long userId, Long bookId, Integer type, String tag, int page, int size) {
        LambdaQueryWrapper<Annotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Annotation::getUserId, userId);
        if (bookId != null) {
            wrapper.eq(Annotation::getBookId, bookId);
        }
        if (type != null) {
            wrapper.eq(Annotation::getType, type);
        }
        if (tag != null && !tag.isEmpty()) {
            wrapper.apply("FIND_IN_SET({0}, tags) > 0", tag);
        }
        wrapper.orderByDesc(Annotation::getIsPinned);
        wrapper.orderByDesc(Annotation::getCreatedAt);
        return annotationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public List<String> listTags(Long userId) {
        LambdaQueryWrapper<Annotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Annotation::getUserId, userId)
                .select(Annotation::getTags)
                .isNotNull(Annotation::getTags)
                .ne(Annotation::getTags, "");
        List<Annotation> annotations = annotationMapper.selectList(wrapper);
        Set<String> tagSet = new HashSet<>();
        for (Annotation ann : annotations) {
            if (ann.getTags() != null && !ann.getTags().isEmpty()) {
                String[] tags = ann.getTags().split(",");
                for (String t : tags) {
                    if (!t.trim().isEmpty()) {
                        tagSet.add(t.trim());
                    }
                }
            }
        }
        return new ArrayList<>(tagSet);
    }

    public void update(Long userId, Long id, AnnotationDTO dto) {
        Annotation ann = annotationMapper.selectById(id);
        if (ann == null) {
            throw new BusinessException("批注不存在");
        }
        TenantValidator.validateCrossTenant(ann.getUserId(), TenantContext.getTenantId());
        ann.setContent(dto.getContent());
        if (dto.getSelectedText() != null) {
            ann.setSelectedText(dto.getSelectedText());
        }
        if (dto.getTags() != null) {
            ann.setTags(dto.getTags());
        }
        if (dto.getIsPinned() != null) {
            ann.setIsPinned(dto.getIsPinned());
        }
        if (dto.getColor() != null) {
            ann.setColor(dto.getColor());
        }
        annotationMapper.updateById(ann);
        log.info("更新批注: id={}", id);
    }

    public void togglePin(Long userId, Long id) {
        Annotation ann = annotationMapper.selectById(id);
        if (ann == null) {
            throw new BusinessException("批注不存在");
        }
        TenantValidator.validateCrossTenant(ann.getUserId(), TenantContext.getTenantId());
        int newPin = ann.getIsPinned() == null || ann.getIsPinned() == 0 ? 1 : 0;
        ann.setIsPinned(newPin);
        annotationMapper.updateById(ann);
        log.info("切换批注置顶: id={}, pinned={}", id, newPin);
    }

    public void updateColor(Long userId, Long id, String color) {
        Annotation ann = annotationMapper.selectById(id);
        if (ann == null) {
            throw new BusinessException("批注不存在");
        }
        TenantValidator.validateCrossTenant(ann.getUserId(), TenantContext.getTenantId());
        ann.setColor(color);
        annotationMapper.updateById(ann);
        log.info("更新批注颜色: id={}, color={}", id, color);
    }

    public void delete(Long userId, Long id) {
        Annotation ann = annotationMapper.selectById(id);
        if (ann == null) {
            throw new BusinessException("批注不存在");
        }
        TenantValidator.validateCrossTenant(ann.getUserId(), TenantContext.getTenantId());
        annotationMapper.deleteById(id);
        log.info("删除批注: id={}", id);
    }

    public String exportMarkdown(Long userId, Long bookId) {
        LambdaQueryWrapper<Annotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Annotation::getUserId, userId)
                .eq(Annotation::getBookId, bookId)
                .orderByAsc(Annotation::getPageNum)
                .orderByDesc(Annotation::getIsPinned)
                .orderByDesc(Annotation::getCreatedAt);
        List<Annotation> annotations = annotationMapper.selectList(wrapper);

        Book book = bookMapper.selectById(bookId);
        String bookTitle = book != null ? book.getTitle() : "未命名书籍";
        String bookAuthor = book != null && book.getAuthor() != null ? book.getAuthor() : "";

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(bookTitle).append("\n\n");
        if (!bookAuthor.isEmpty()) {
            sb.append("作者：").append(bookAuthor).append("\n\n");
        }
        sb.append("---\n\n");

        Map<Integer, List<Annotation>> pageMap = annotations.stream()
                .collect(Collectors.groupingBy(Annotation::getPageNum, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Integer, List<Annotation>> entry : pageMap.entrySet()) {
            Integer pageNum = entry.getKey();
            List<Annotation> pageAnnotations = entry.getValue();
            sb.append("## 第 ").append(pageNum).append(" 页\n\n");

            for (Annotation ann : pageAnnotations) {
                if (ann.getSelectedText() != null && !ann.getSelectedText().isEmpty()) {
                    sb.append("> ").append(ann.getSelectedText()).append("\n\n");
                }
                if (ann.getContent() != null && !ann.getContent().isEmpty()) {
                    sb.append(ann.getContent()).append("\n\n");
                }
                if (ann.getTags() != null && !ann.getTags().isEmpty()) {
                    String[] tags = ann.getTags().split(",");
                    for (String tag : tags) {
                        if (!tag.trim().isEmpty()) {
                            sb.append("#").append(tag.trim()).append(" ");
                        }
                    }
                    sb.append("\n\n");
                }
                sb.append("---\n\n");
            }
        }

        return sb.toString();
    }

    public List<Annotation> listAllByBook(Long userId, Long bookId) {
        LambdaQueryWrapper<Annotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Annotation::getUserId, userId)
                .eq(Annotation::getBookId, bookId)
                .orderByDesc(Annotation::getIsPinned)
                .orderByAsc(Annotation::getPageNum)
                .orderByDesc(Annotation::getCreatedAt);
        return annotationMapper.selectList(wrapper);
    }
}
