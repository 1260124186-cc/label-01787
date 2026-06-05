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
}
