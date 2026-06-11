package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.dto.BookmarkDTO;
import com.xiaoan.bookstore.entity.Bookmark;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.BookmarkMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private static final Logger log = LoggerFactory.getLogger(BookmarkService.class);
    private final BookmarkMapper bookmarkMapper;

    public Bookmark create(Long userId, BookmarkDTO dto) {
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId(userId);
        bookmark.setBookId(dto.getBookId());
        bookmark.setBookTitle(dto.getBookTitle() != null ? dto.getBookTitle() : "");
        bookmark.setPageNum(dto.getPageNum());
        bookmark.setUnitType(dto.getUnitType() != null ? dto.getUnitType() : 1);
        bookmark.setTitle(dto.getTitle() != null ? dto.getTitle() : "");
        bookmark.setRemark(dto.getRemark() != null ? dto.getRemark() : "");
        bookmark.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        bookmark.setIsChapter(dto.getIsChapter() != null ? dto.getIsChapter() : 0);
        bookmarkMapper.insert(bookmark);
        log.info("添加书签: userId={}, bookId={}, pageNum={}", userId, dto.getBookId(), dto.getPageNum());
        return bookmark;
    }

    public List<Bookmark> listByBook(Long userId, Long bookId, Integer isChapter) {
        LambdaQueryWrapper<Bookmark> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bookmark::getUserId, userId)
                .eq(Bookmark::getBookId, bookId);
        if (isChapter != null) {
            wrapper.eq(Bookmark::getIsChapter, isChapter);
        }
        wrapper.orderByAsc(Bookmark::getSortOrder)
                .orderByAsc(Bookmark::getPageNum)
                .orderByDesc(Bookmark::getCreatedAt);
        return bookmarkMapper.selectList(wrapper);
    }

    public Map<String, Object> listGroupByBook(Long userId) {
        LambdaQueryWrapper<Bookmark> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bookmark::getUserId, userId)
                .orderByDesc(Bookmark::getCreatedAt);
        List<Bookmark> all = bookmarkMapper.selectList(wrapper);

        Map<Long, Map<String, Object>> grouped = new HashMap<>();
        for (Bookmark bm : all) {
            Long bookId = bm.getBookId();
            Map<String, Object> group = grouped.computeIfAbsent(bookId, k -> {
                Map<String, Object> g = new HashMap<>();
                g.put("bookId", bookId);
                g.put("bookTitle", bm.getBookTitle());
                g.put("list", new ArrayList<Bookmark>());
                return g;
            });
            ((List<Bookmark>) group.get("list")).add(bm);
        }

        List<Map<String, Object>> result = new ArrayList<>(grouped.values());
        result.sort((a, b) -> {
            List<Bookmark> la = (List<Bookmark>) a.get("list");
            List<Bookmark> lb = (List<Bookmark>) b.get("list");
            return lb.get(0).getCreatedAt().compareTo(la.get(0).getCreatedAt());
        });

        Map<String, Object> data = new HashMap<>();
        data.put("groups", result);
        data.put("total", all.size());
        return data;
    }

    public void update(Long userId, Long id, BookmarkDTO dto) {
        Bookmark bookmark = bookmarkMapper.selectById(id);
        if (bookmark == null) {
            throw new BusinessException("书签不存在");
        }
        TenantValidator.validateCrossTenant(bookmark.getUserId(), TenantContext.getTenantId());
        if (dto.getTitle() != null) {
            bookmark.setTitle(dto.getTitle());
        }
        if (dto.getRemark() != null) {
            bookmark.setRemark(dto.getRemark());
        }
        if (dto.getSortOrder() != null) {
            bookmark.setSortOrder(dto.getSortOrder());
        }
        if (dto.getIsChapter() != null) {
            bookmark.setIsChapter(dto.getIsChapter());
        }
        bookmarkMapper.updateById(bookmark);
        log.info("更新书签: id={}", id);
    }

    @Transactional
    public void reorder(Long userId, List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            LambdaUpdateWrapper<Bookmark> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Bookmark::getId, ids.get(i))
                    .eq(Bookmark::getUserId, userId)
                    .set(Bookmark::getSortOrder, i);
            bookmarkMapper.update(null, wrapper);
        }
        log.info("书签重排序: userId={}, count={}", userId, ids.size());
    }

    public void delete(Long userId, Long id) {
        Bookmark bookmark = bookmarkMapper.selectById(id);
        if (bookmark == null) {
            throw new BusinessException("书签不存在");
        }
        TenantValidator.validateCrossTenant(bookmark.getUserId(), TenantContext.getTenantId());
        bookmarkMapper.deleteById(id);
        log.info("删除书签: id={}", id);
    }

    public Bookmark getByBookAndPage(Long userId, Long bookId, Integer pageNum) {
        LambdaQueryWrapper<Bookmark> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bookmark::getUserId, userId)
                .eq(Bookmark::getBookId, bookId)
                .eq(Bookmark::getPageNum, pageNum)
                .last("LIMIT 1");
        return bookmarkMapper.selectOne(wrapper);
    }
}
