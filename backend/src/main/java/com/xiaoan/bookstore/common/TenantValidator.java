package com.xiaoan.bookstore.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.exception.BusinessException;

public class TenantValidator {

    public static <T> T validateOwnership(BaseMapper<T> mapper, Long id, Long expectedUserId) {
        T entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("资源不存在");
        }
        try {
            Object ownerId = entity.getClass().getMethod("getUserId").invoke(entity);
            if (ownerId == null || !ownerId.equals(expectedUserId)) {
                throw new BusinessException(403, "无权访问该资源");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("租户校验失败");
        }
        return entity;
    }

    public static void validateCrossTenant(Long resourceUserId, Long currentUserId) {
        if (resourceUserId == null || !resourceUserId.equals(currentUserId)) {
            throw new BusinessException(403, "无权访问该资源");
        }
    }
}
