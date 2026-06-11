package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.dto.CategoryDTO;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.entity.Category;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.BookMapper;
import com.xiaoan.bookstore.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);
    private final CategoryMapper categoryMapper;
    private final BookMapper bookMapper;

    public Category create(Long userId, CategoryDTO dto) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(dto.getName());
        category.setColor(dto.getColor() != null ? dto.getColor() : "");
        category.setIcon(dto.getIcon() != null ? dto.getIcon() : "");
        Integer maxSort = getMaxSortOrder(userId);
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : (maxSort != null ? maxSort + 1 : 0));
        categoryMapper.insert(category);
        log.info("创建分类: userId={}, name={}", userId, dto.getName());
        return category;
    }

    private Integer getMaxSortOrder(Long userId) {
        List<Category> list = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, userId)
                        .orderByDesc(Category::getSortOrder)
                        .last("LIMIT 1")
        );
        return list.isEmpty() ? null : list.get(0).getSortOrder();
    }

    public List<Category> list(Long userId) {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, userId)
                        .orderByAsc(Category::getSortOrder)
                        .orderByDesc(Category::getCreatedAt)
        );
    }

    public void update(Long userId, Long id, CategoryDTO dto) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        TenantValidator.validateCrossTenant(category.getUserId(), TenantContext.getTenantId());
        category.setName(dto.getName());
        if (dto.getColor() != null) {
            category.setColor(dto.getColor());
        }
        if (dto.getIcon() != null) {
            category.setIcon(dto.getIcon());
        }
        if (dto.getSortOrder() != null) {
            category.setSortOrder(dto.getSortOrder());
        }
        categoryMapper.updateById(category);
        log.info("更新分类: id={}, name={}", id, dto.getName());
    }

    public long countBooks(Long userId, Long categoryId) {
        return bookMapper.selectCount(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getUserId, userId)
                        .eq(Book::getCategoryId, categoryId)
                        .eq(Book::getStatus, 1)
        );
    }

    @Transactional
    public void delete(Long userId, Long id, boolean moveToUncategorized) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        TenantValidator.validateCrossTenant(category.getUserId(), TenantContext.getTenantId());

        long bookCount = countBooks(userId, id);
        if (bookCount > 0 && !moveToUncategorized) {
            throw new BusinessException("该分类下有 " + bookCount + " 本书，请选择移至未分类后再删除");
        }

        if (bookCount > 0 && moveToUncategorized) {
            bookMapper.update(null,
                    new LambdaUpdateWrapper<Book>()
                            .eq(Book::getCategoryId, id)
                            .eq(Book::getUserId, userId)
                            .set(Book::getCategoryId, null)
            );
            log.info("分类下 {} 本书移至未分类: categoryId={}", bookCount, id);
        }

        categoryMapper.deleteById(id);
        log.info("删除分类: id={}", id);
    }

    public Category getById(Long userId, Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            return null;
        }
        if (!category.getUserId().equals(userId)) {
            return null;
        }
        return category;
    }

    @Transactional
    public void batchSort(Long userId, List<Map<String, Object>> sortList) {
        for (Map<String, Object> item : sortList) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
            Category category = categoryMapper.selectById(id);
            if (category != null && category.getUserId().equals(userId)) {
                category.setSortOrder(sortOrder);
                categoryMapper.updateById(category);
            }
        }
        log.info("批量更新分类排序: userId={}, count={}", userId, sortList.size());
    }
}
