package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.NotificationVO;
import com.xiaoan.bookstore.dto.SendAnnouncementDTO;
import com.xiaoan.bookstore.entity.Notification;
import com.xiaoan.bookstore.entity.NotificationTemplate;
import com.xiaoan.bookstore.entity.User;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.NotificationMapper;
import com.xiaoan.bookstore.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    private final NotificationMapper notificationMapper;
    private final NotificationTemplateService templateService;
    private final UserMapper userMapper;

    private static final Map<Integer, String> TYPE_NAMES = new HashMap<>();
    static {
        TYPE_NAMES.put(1, "系统通知");
        TYPE_NAMES.put(2, "审核结果");
        TYPE_NAMES.put(3, "计划提醒");
        TYPE_NAMES.put(4, "小组动态");
    }

    public IPage<NotificationVO> getMyNotifications(Long userId, Integer type, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Notification::getUserId, userId).or().isNull(Notification::getUserId));
        if (type != null && type > 0) {
            wrapper.eq(Notification::getType, type);
        }
        wrapper.orderByDesc(Notification::getCreatedAt);

        Page<Notification> notificationPage = notificationMapper.selectPage(new Page<>(page, size), wrapper);
        
        Page<NotificationVO> voPage = new Page<>(notificationPage.getCurrent(), notificationPage.getSize(), notificationPage.getTotal());
        List<NotificationVO> voList = new ArrayList<>();
        for (Notification notification : notificationPage.getRecords()) {
            voList.add(convertToVO(notification));
        }
        voPage.setRecords(voList);
        return voPage;
    }

    public Map<Integer, Long> getUnreadCount(Long userId) {
        Map<Integer, Long> countMap = new HashMap<>();
        for (int type = 1; type <= 4; type++) {
            LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
            wrapper.and(w -> w.eq(Notification::getUserId, userId).or().isNull(Notification::getUserId));
            wrapper.eq(Notification::getType, type);
            wrapper.eq(Notification::getIsRead, 0);
            Long count = notificationMapper.selectCount(wrapper);
            countMap.put(type, count);
        }
        
        LambdaQueryWrapper<Notification> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.and(w -> w.eq(Notification::getUserId, userId).or().isNull(Notification::getUserId));
        totalWrapper.eq(Notification::getIsRead, 0);
        Long total = notificationMapper.selectCount(totalWrapper);
        countMap.put(0, total);
        
        return countMap;
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        int rows = notificationMapper.markAsRead(id, userId);
        if (rows == 0) {
            throw new BusinessException("消息不存在或无权限");
        }
    }

    @Transactional
    public void markAllAsRead(Long userId, Integer type) {
        if (type != null && type > 0) {
            notificationMapper.markAllAsReadByType(userId, type);
        } else {
            notificationMapper.markAllAsRead(userId);
        }
    }

    public NotificationVO getDetail(Long id, Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getId, id);
        wrapper.and(w -> w.eq(Notification::getUserId, userId).or().isNull(Notification::getUserId));
        Notification notification = notificationMapper.selectOne(wrapper);
        if (notification == null) {
            throw new BusinessException("消息不存在或无权限");
        }
        if (notification.getIsRead() == 0) {
            markAsRead(id, userId);
            notification.setIsRead(1);
            notification.setReadAt(LocalDateTime.now());
        }
        return convertToVO(notification);
    }

    @Transactional
    public void sendAnnouncement(SendAnnouncementDTO dto, Long senderId) {
        if (dto.getType() < 1 || dto.getType() > 4) {
            throw new BusinessException("无效的消息类型");
        }

        if (Boolean.TRUE.equals(dto.getSendToAll())) {
            Notification notification = new Notification();
            notification.setUserId(null);
            notification.setType(dto.getType());
            notification.setTitle(dto.getTitle());
            notification.setContent(dto.getContent());
            notification.setExtraData(dto.getExtraData());
            notification.setIsRead(0);
            notification.setSenderId(senderId);
            notificationMapper.insert(notification);
            log.info("发送全体公告: type={}, title={}, senderId={}", dto.getType(), dto.getTitle(), senderId);
        } else {
            List<Long> userIds = dto.getUserIds();
            if (userIds == null || userIds.isEmpty()) {
                throw new BusinessException("请选择接收用户");
            }
            for (Long userId : userIds) {
                User user = userMapper.selectById(userId);
                if (user == null || user.getStatus() != Constants.STATUS_ENABLED) {
                    continue;
                }
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setType(dto.getType());
                notification.setTitle(dto.getTitle());
                notification.setContent(dto.getContent());
                notification.setExtraData(dto.getExtraData());
                notification.setIsRead(0);
                notification.setSenderId(senderId);
                notificationMapper.insert(notification);
            }
            log.info("发送指定用户公告: type={}, title={}, userCount={}, senderId={}", 
                dto.getType(), dto.getTitle(), userIds.size(), senderId);
        }
    }

    public void sendByTemplate(String templateCode, Long userId, Map<String, String> variables, String extraData) {
        NotificationTemplate template = templateService.getByCode(templateCode);
        if (template == null || template.getStatus() != 1) {
            log.warn("模板不存在或已禁用: {}", templateCode);
            return;
        }

        String content = template.getContent();
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                content = content.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(template.getType());
        notification.setTitle(template.getTitle());
        notification.setContent(content);
        notification.setExtraData(extraData);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
        
        log.info("通过模板发送消息: templateCode={}, userId={}", templateCode, userId);
    }

    public IPage<Notification> getAdminNotifications(Integer type, Integer isRead, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        if (type != null && type > 0) {
            wrapper.eq(Notification::getType, type);
        }
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }
        wrapper.orderByDesc(Notification::getCreatedAt);
        return notificationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private NotificationVO convertToVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setType(notification.getType());
        vo.setTypeName(TYPE_NAMES.getOrDefault(notification.getType(), "未知"));
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setExtraData(notification.getExtraData());
        vo.setIsRead(notification.getIsRead());
        vo.setCreatedAt(notification.getCreatedAt());
        vo.setReadAt(notification.getReadAt());
        return vo;
    }
}
