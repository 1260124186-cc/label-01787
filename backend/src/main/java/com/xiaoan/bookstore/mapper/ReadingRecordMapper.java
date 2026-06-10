package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.ReadingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReadingRecordMapper extends BaseMapper<ReadingRecord> {

    @Select("<script>SELECT COALESCE(SUM(duration), 0) FROM reading_record WHERE start_time &gt;= #{start} AND start_time &lt; #{end} <if test='userId != null'>AND user_id = #{userId}</if></script>")
    Long sumDuration(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(DISTINCT book_id) FROM reading_record WHERE user_id = #{userId} AND start_time >= #{start} AND start_time < #{end}")
    Integer countBooks(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT DATE(start_time) as date, SUM(duration) as total FROM reading_record WHERE user_id = #{userId} AND start_time >= #{start} AND start_time < #{end} GROUP BY DATE(start_time) ORDER BY date")
    List<Map<String, Object>> dailyDuration(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT b.id as bookId, b.title as bookTitle, SUM(r.duration) as totalDuration, COUNT(*) as readCount " +
            "FROM reading_record r " +
            "LEFT JOIN book b ON r.book_id = b.id " +
            "WHERE r.user_id = #{userId} AND r.start_time >= #{start} AND r.start_time < #{end} " +
            "GROUP BY r.book_id " +
            "ORDER BY totalDuration DESC " +
            "LIMIT 10")
    List<Map<String, Object>> bookRank(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT c.id as categoryId, c.name as categoryName, SUM(r.duration) as totalDuration, COUNT(DISTINCT r.book_id) as bookCount " +
            "FROM reading_record r " +
            "LEFT JOIN book b ON r.book_id = b.id " +
            "LEFT JOIN category c ON b.category_id = c.id " +
            "WHERE r.user_id = #{userId} AND r.start_time >= #{start} AND r.start_time < #{end} " +
            "GROUP BY b.category_id " +
            "ORDER BY totalDuration DESC")
    List<Map<String, Object>> categoryStats(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(DISTINCT DATE(start_time)) FROM reading_record WHERE user_id = #{userId} AND start_time >= #{start} AND start_time < #{end}")
    Integer countReadingDays(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT DATE(start_time) as date, SUM(duration) as total FROM reading_record WHERE user_id = #{userId} AND start_time >= #{start} AND start_time < #{end} GROUP BY DATE(start_time) ORDER BY total DESC LIMIT 1")
    Map<String, Object> maxDayDuration(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
