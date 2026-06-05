package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.dto.AnnotationDTO;
import com.xiaoan.bookstore.entity.Annotation;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.AnnotationMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnnotationService {

    private static final Logger log = LoggerFactory.getLogger(AnnotationService.class);
    private final AnnotationMapper annotationMapper;

    public Annotation create(Long userId, AnnotationDTO dto) {
        Annotation ann = new Annotation();
        ann.setUserId(userId);
        ann.setBookId(dto.getBookId());
        ann.setPageNum(dto.getPageNum());
        ann.setSelectedText(dto.getSelectedText());
        ann.setContent(dto.getContent());
        ann.setType(dto.getType());
        annotationMapper.insert(ann);
        log.info("添加批注: userId={}, bookId={}, page={}", userId, dto.getBookId(), dto.getPageNum());
        return ann;
    }

    public Page<Annotation> list(Long userId, Long bookId, Integer type, int page, int size) {
        LambdaQueryWrapper<Annotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Annotation::getUserId, userId);
        if (bookId != null) {
            wrapper.eq(Annotation::getBookId, bookId);
        }
        if (type != null) {
            wrapper.eq(Annotation::getType, type);
        }
        wrapper.orderByDesc(Annotation::getCreatedAt);
        return annotationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public void update(Long userId, Long id, AnnotationDTO dto) {
        Annotation ann = annotationMapper.selectById(id);
        if (ann == null || !ann.getUserId().equals(userId)) {
            throw new BusinessException("批注不存在");
        }
        ann.setContent(dto.getContent());
        if (dto.getSelectedText() != null) {
            ann.setSelectedText(dto.getSelectedText());
        }
        annotationMapper.updateById(ann);
        log.info("更新批注: id={}", id);
    }

    public void delete(Long userId, Long id) {
        Annotation ann = annotationMapper.selectById(id);
        if (ann == null || !ann.getUserId().equals(userId)) {
            throw new BusinessException("批注不存在");
        }
        annotationMapper.deleteById(id);
        log.info("删除批注: id={}", id);
    }
}
