package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.dto.InkStrokeDTO;
import com.xiaoan.bookstore.entity.InkStroke;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.InkStrokeMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InkStrokeService {

    private static final Logger log = LoggerFactory.getLogger(InkStrokeService.class);
    private final InkStrokeMapper inkStrokeMapper;

    public InkStroke saveStroke(Long userId, InkStrokeDTO dto) {
        InkStroke existing = inkStrokeMapper.selectOne(
            new LambdaQueryWrapper<InkStroke>()
                .eq(InkStroke::getUserId, userId)
                .eq(InkStroke::getBookId, dto.getBookId())
                .eq(InkStroke::getStrokeId, dto.getStrokeId())
        );

        if (existing != null) {
            existing.setStrokeType(dto.getStrokeType());
            existing.setColor(dto.getColor());
            existing.setLineWidth(dto.getLineWidth());
            existing.setOpacity(dto.getOpacity());
            existing.setPoints(dto.getPoints());
            existing.setBoundingBox(dto.getBoundingBox());
            existing.setPageNum(dto.getPageNum());
            inkStrokeMapper.updateById(existing);
            log.info("更新墨迹笔迹: strokeId={}, userId={}", dto.getStrokeId(), userId);
            return existing;
        }

        InkStroke stroke = new InkStroke();
        stroke.setUserId(userId);
        stroke.setBookId(dto.getBookId());
        stroke.setPageNum(dto.getPageNum());
        stroke.setStrokeId(dto.getStrokeId());
        stroke.setStrokeType(dto.getStrokeType() != null ? dto.getStrokeType() : "pen");
        stroke.setColor(dto.getColor() != null ? dto.getColor() : "#000000");
        stroke.setLineWidth(dto.getLineWidth() != null ? dto.getLineWidth() : 2.0);
        stroke.setOpacity(dto.getOpacity() != null ? dto.getOpacity() : 1.0);
        stroke.setPoints(dto.getPoints());
        stroke.setBoundingBox(dto.getBoundingBox());
        inkStrokeMapper.insert(stroke);
        log.info("保存墨迹笔迹: strokeId={}, userId={}, bookId={}, page={}", dto.getStrokeId(), userId, dto.getBookId(), dto.getPageNum());
        return stroke;
    }

    @Transactional
    public Map<String, Object> batchSync(Long userId, Long bookId, Integer pageNum,
                                           List<InkStrokeDTO> strokes, List<String> deletedStrokeIds) {
        int saved = 0;
        int deleted = 0;

        if (strokes != null) {
            for (InkStrokeDTO dto : strokes) {
                dto.setBookId(bookId);
                dto.setPageNum(pageNum);
                saveStroke(userId, dto);
                saved++;
            }
        }

        if (deletedStrokeIds != null) {
            for (String strokeId : deletedStrokeIds) {
                int rows = inkStrokeMapper.deleteByStrokeId(userId, bookId, strokeId);
                deleted += rows;
            }
        }

        log.info("墨迹批量同步: bookId={}, page={}, saved={}, deleted={}, userId={}", bookId, pageNum, saved, deleted, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("saved", saved);
        result.put("deleted", deleted);
        result.put("strokes", getByPage(userId, bookId, pageNum));
        return result;
    }

    public List<InkStroke> getByPage(Long userId, Long bookId, Integer pageNum) {
        return inkStrokeMapper.selectByPage(userId, bookId, pageNum);
    }

    public List<InkStroke> getByBook(Long userId, Long bookId) {
        return inkStrokeMapper.selectByBook(userId, bookId);
    }

    public List<InkStroke> getByBookAndPages(Long userId, Long bookId, List<Integer> pageNums) {
        if (pageNums == null || pageNums.isEmpty()) {
            return getByBook(userId, bookId);
        }
        LambdaQueryWrapper<InkStroke> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InkStroke::getUserId, userId)
                .eq(InkStroke::getBookId, bookId)
                .in(InkStroke::getPageNum, pageNums)
                .orderByAsc(InkStroke::getPageNum)
                .orderByAsc(InkStroke::getCreatedAt);
        return inkStrokeMapper.selectList(wrapper);
    }

    public List<Map<String, Object>> getPageStats(Long userId, Long bookId) {
        return inkStrokeMapper.selectPageStats(userId, bookId);
    }

    public void deleteStroke(Long userId, Long id) {
        InkStroke stroke = inkStrokeMapper.selectById(id);
        if (stroke == null) {
            throw new BusinessException("笔迹不存在");
        }
        TenantValidator.validateCrossTenant(stroke.getUserId(), TenantContext.getTenantId());
        inkStrokeMapper.deleteById(id);
        log.info("删除墨迹笔迹: id={}, strokeId={}", id, stroke.getStrokeId());
    }

    public void deleteByStrokeId(Long userId, Long bookId, String strokeId) {
        int rows = inkStrokeMapper.deleteByStrokeId(userId, bookId, strokeId);
        if (rows > 0) {
            log.info("删除墨迹笔迹: strokeId={}", strokeId);
        }
    }

    @Transactional
    public void clearPage(Long userId, Long bookId, Integer pageNum) {
        LambdaQueryWrapper<InkStroke> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InkStroke::getUserId, userId)
                .eq(InkStroke::getBookId, bookId)
                .eq(InkStroke::getPageNum, pageNum);
        inkStrokeMapper.delete(wrapper);
        log.info("清空页面墨迹: bookId={}, page={}, userId={}", bookId, pageNum, userId);
    }

    @Transactional
    public void clearBook(Long userId, Long bookId) {
        LambdaQueryWrapper<InkStroke> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InkStroke::getUserId, userId)
                .eq(InkStroke::getBookId, bookId);
        inkStrokeMapper.delete(wrapper);
        log.info("清空书籍墨迹: bookId={}, userId={}", bookId, userId);
    }

    public Map<Integer, List<InkStroke>> groupByPage(List<InkStroke> strokes) {
        return strokes.stream()
                .collect(Collectors.groupingBy(InkStroke::getPageNum, LinkedHashMap::new, Collectors.toList()));
    }
}
