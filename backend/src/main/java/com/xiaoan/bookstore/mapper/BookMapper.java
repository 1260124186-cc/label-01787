package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    @Select("SELECT DATE(created_at) as date, COUNT(*) as uploadCount " +
            "FROM book " +
            "WHERE created_at >= #{start} AND created_at < #{end} " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> uploadTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT DATE(created_at) as date, COUNT(DISTINCT user_id) as userCount " +
            "FROM book " +
            "WHERE created_at >= #{start} AND created_at < #{end} " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> uploadUserTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
