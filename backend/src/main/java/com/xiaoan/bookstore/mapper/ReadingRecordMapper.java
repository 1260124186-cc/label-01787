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

    @Select("SELECT b.id as bookId, b.title as bookTitle, b.author as bookAuthor, " +
            "b.book_format as bookFormat, " +
            "b.page_count as pageCount, b.chapter_count as chapterCount, " +
            "b.last_page as lastPage, b.last_chapter as lastChapter, " +
            "MAX(r.start_time) as lastReadTime, " +
            "SUM(r.duration) as totalDuration, COUNT(r.id) as readCount " +
            "FROM book b " +
            "LEFT JOIN reading_record r ON b.id = r.book_id AND r.user_id = #{userId} " +
            "WHERE b.user_id = #{userId} AND b.status = 1 " +
            "AND ((b.book_format = 'epub' AND b.last_chapter < GREATEST(b.chapter_count, 1)) " +
            "     OR (b.book_format != 'epub' AND b.last_page < GREATEST(b.page_count, 1))) " +
            "GROUP BY b.id " +
            "HAVING lastReadTime IS NOT NULL " +
            "ORDER BY lastReadTime DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> continueReadingList(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Select("SELECT DATE(r.start_time) as date, " +
            "b.id as bookId, b.title as bookTitle, b.author as bookAuthor, " +
            "SUM(r.duration) as duration, MAX(r.last_page) as lastPage " +
            "FROM reading_record r " +
            "LEFT JOIN book b ON r.book_id = b.id " +
            "WHERE r.user_id = #{userId} AND r.start_time >= #{start} AND r.start_time < #{end} " +
            "AND r.duration > 0 " +
            "GROUP BY DATE(r.start_time), r.book_id " +
            "ORDER BY date DESC, duration DESC")
    List<Map<String, Object>> readingTimeline(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(DISTINCT user_id) FROM reading_record " +
            "WHERE start_time >= #{start} AND start_time < #{end} AND duration > 0")
    Long countActiveUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(AVG(daily_total), 0) FROM (" +
            "    SELECT user_id, SUM(duration) as daily_total " +
            "    FROM reading_record " +
            "    WHERE start_time >= #{start} AND start_time < #{end} AND duration > 0 " +
            "    GROUP BY user_id, DATE(start_time)" +
            ") as user_daily")
    Long avgDailyDurationPerUser(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT DATE(start_time) as date, COUNT(DISTINCT user_id) as userCount, " +
            "SUM(duration) as totalDuration, COUNT(DISTINCT book_id) as bookCount " +
            "FROM reading_record " +
            "WHERE start_time >= #{start} AND start_time < #{end} AND duration > 0 " +
            "GROUP BY DATE(start_time) " +
            "ORDER BY date DESC")
    List<Map<String, Object>> dailyReadingStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(DISTINCT user_id) FROM reading_record " +
            "WHERE DATE(start_time) = DATE_SUB(CURDATE(), INTERVAL #{days} DAY) AND duration > 0")
    Long countActiveUsersByDay(@Param("days") int daysAgo);

    @Select("SELECT COUNT(DISTINCT r1.user_id) FROM reading_record r1 " +
            "INNER JOIN reading_record r2 ON r1.user_id = r2.user_id " +
            "WHERE DATE(r1.start_time) = DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "AND DATE(r2.start_time) = CURDATE() " +
            "AND r1.duration > 0 AND r2.duration > 0")
    Long countRetentionUsers(@Param("days") int daysAgo);

    @Select("SELECT DATE(start_time) as date, COUNT(DISTINCT user_id) as dau " +
            "FROM reading_record " +
            "WHERE start_time >= #{start} AND start_time < #{end} AND duration > 0 " +
            "GROUP BY DATE(start_time) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> dauTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT DATE(start_time) as date, COUNT(*) as readCount, " +
            "COUNT(DISTINCT user_id) as userCount, " +
            "COUNT(DISTINCT book_id) as bookCount, " +
            "SUM(duration) as totalDuration " +
            "FROM reading_record " +
            "WHERE start_time >= #{start} AND start_time < #{end} " +
            "GROUP BY DATE(start_time) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> readingTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
