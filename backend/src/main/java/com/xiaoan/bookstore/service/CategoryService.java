package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoan.bookstore.dto.CategoryDTO;
import com.xiaoan.bookstore.entity.Category;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);
    private final CategoryMapper categoryMapper;

    public Category create(Long userId, CategoryDTO dto) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(dto.getName());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        categoryMapper.insert(category);
        log.info("创建分类: userId={}, name={}", userId, dto.getName());
        return category;
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
        if (category == null || !category.getUserId().equals(userId)) {
            throw new BusinessException("分类不存在");
        }
        category.setName(dto.getName());
        if (dto.getSortOrder() != null) {
            category.setSortOrder(dto.getSortOrder());
        }
        categoryMapper.updateById(category);
        log.info("更新分类: id={}, name={}", id, dto.getName());
    }

    public void delete(Long userId, Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null || !category.getUserId().equals(userId)) {
            throw new BusinessException("分类不存在");
        }
        categoryMapper.deleteById(id);
        log.info("删除分类: id={}", id);
    }
}
