package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    
    @Update("UPDATE notification SET is_read = 1, read_at = NOW() WHERE id = #{id} AND (user_id = #{userId} OR user_id IS NULL)")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);
    
    @Update("UPDATE notification SET is_read = 1, read_at = NOW() WHERE (user_id = #{userId} OR user_id IS NULL) AND is_read = 0")
    int markAllAsRead(@Param("userId") Long userId);
    
    @Update("UPDATE notification SET is_read = 1, read_at = NOW() WHERE (user_id = #{userId} OR user_id IS NULL) AND type = #{type} AND is_read = 0")
    int markAllAsReadByType(@Param("userId") Long userId, @Param("type") Integer type);
}
