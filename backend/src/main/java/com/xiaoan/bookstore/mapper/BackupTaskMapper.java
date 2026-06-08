package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.BackupTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BackupTaskMapper extends BaseMapper<BackupTask> {

    @Select("SELECT COALESCE(SUM(file_size), 0) FROM book WHERE status = 1")
    Long getTotalFileSize();

    @Select("SELECT user_id, COALESCE(SUM(file_size), 0) as total_size, COUNT(*) as book_count " +
            "FROM book WHERE status = 1 GROUP BY user_id ORDER BY total_size DESC LIMIT 10")
    List<java.util.Map<String, Object>> getTopUsersByStorage();

    @Select("SELECT DATE(created_at) as date, COALESCE(SUM(file_size), 0) as total_size, " +
            "COUNT(*) as book_count, COUNT(DISTINCT user_id) as user_count " +
            "FROM book WHERE status = 1 AND created_at >= #{startDate} " +
            "GROUP BY DATE(created_at) ORDER BY date DESC")
    List<java.util.Map<String, Object>> getDailyStorageTrend(@Param("startDate") LocalDateTime startDate);
}
