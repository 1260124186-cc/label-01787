package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.dto.NotificationTemplateDTO;
import com.xiaoan.bookstore.entity.NotificationTemplate;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.NotificationTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private static final Logger log = LoggerFactory.getLogger(NotificationTemplateService.class);
    
    private final NotificationTemplateMapper templateMapper;

    public IPage<NotificationTemplate> getTemplates(Integer type, Integer status, int page, int size) {
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        if (type != null && type > 0) {
            wrapper.eq(NotificationTemplate::getType, type);
        }
        if (status != null) {
            wrapper.eq(NotificationTemplate::getStatus, status);
        }
        wrapper.orderByDesc(NotificationTemplate::getCreatedAt);
        return templateMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public List<NotificationTemplate> getAllEnabledTemplates() {
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationTemplate::getStatus, 1);
        wrapper.orderByAsc(NotificationTemplate::getType);
        return templateMapper.selectList(wrapper);
    }

    public NotificationTemplate getById(Long id) {
        return templateMapper.selectById(id);
    }

    public NotificationTemplate getByCode(String code) {
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationTemplate::getCode, code);
        return templateMapper.selectOne(wrapper);
    }

    @Transactional
    public NotificationTemplate create(NotificationTemplateDTO dto) {
        NotificationTemplate existing = getByCode(dto.getCode());
        if (existing != null) {
            throw new BusinessException("模板编码已存在");
        }

        NotificationTemplate template = new NotificationTemplate();
        template.setCode(dto.getCode());
        template.setName(dto.getName());
        template.setType(dto.getType());
        template.setTitle(dto.getTitle());
        template.setContent(dto.getContent());
        template.setStatus(dto.getStatus() != null ? dto.getStatus() : Constants.STATUS_ENABLED);
        
        templateMapper.insert(template);
        log.info("创建消息模板: code={}, name={}", dto.getCode(), dto.getName());
        return template;
    }

    @Transactional
    public NotificationTemplate update(Long id, NotificationTemplateDTO dto) {
        NotificationTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        if (!template.getCode().equals(dto.getCode())) {
            NotificationTemplate existing = getByCode(dto.getCode());
            if (existing != null) {
                throw new BusinessException("模板编码已存在");
            }
        }

        template.setCode(dto.getCode());
        template.setName(dto.getName());
        template.setType(dto.getType());
        template.setTitle(dto.getTitle());
        template.setContent(dto.getContent());
        if (dto.getStatus() != null) {
            template.setStatus(dto.getStatus());
        }
        
        templateMapper.updateById(template);
        log.info("更新消息模板: id={}, code={}", id, dto.getCode());
        return template;
    }

    @Transactional
    public void delete(Long id) {
        NotificationTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        templateMapper.deleteById(id);
        log.info("删除消息模板: id={}, code={}", id, template.getCode());
    }

    @Transactional
    public void toggleStatus(Long id) {
        NotificationTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        template.setStatus(template.getStatus() == 1 ? 0 : 1);
        templateMapper.updateById(template);
        log.info("切换模板状态: id={}, code={}, status={}", id, template.getCode(), template.getStatus());
    }
}
