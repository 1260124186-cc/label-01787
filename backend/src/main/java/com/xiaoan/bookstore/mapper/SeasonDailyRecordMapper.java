package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.SeasonDailyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface SeasonDailyRecordMapper extends BaseMapper<SeasonDailyRecord> {

    @Select("SELECT * FROM season_daily_record WHERE season_id = #{seasonId} AND user_id = #{userId} AND record_date = #{date}")
    SeasonDailyRecord findBySeasonUserDate(@Param("seasonId") Long seasonId,
                                            @Param("userId") Long userId,
                                            @Param("date") LocalDate date);

    @Select("SELECT record_date FROM season_daily_record WHERE season_id = #{seasonId} AND user_id = #{userId} AND is_qualified = 1 ORDER BY record_date")
    List<LocalDate> listQualifiedDates(@Param("seasonId") Long seasonId, @Param("userId") Long userId);

    @Select("SELECT record_date FROM season_daily_record WHERE season_id = #{seasonId} AND user_id = #{userId} ORDER BY record_date")
    List<LocalDate> listRecordDates(@Param("seasonId") Long seasonId, @Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(duration), 0) FROM season_daily_record WHERE season_id = #{seasonId} AND user_id = #{userId}")
    Long sumDurationBySeasonUser(@Param("seasonId") Long seasonId, @Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT book_id) FROM reading_record WHERE user_id = #{userId} AND DATE(start_time) BETWEEN #{startDate} AND #{endDate} AND duration > 60")
    Integer countDistinctBooks(@Param("userId") Long userId,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

    @Select("SELECT COALESCE(SUM(duration), 0) FROM reading_record WHERE user_id = #{userId} AND DATE(start_time) = #{date} AND duration > 0")
    Integer sumDailyReadingDuration(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT record_date as date, duration, book_count, is_qualified, is_flagged " +
            "FROM season_daily_record WHERE season_id = #{seasonId} AND user_id = #{userId} " +
            "ORDER BY record_date")
    List<Map<String, Object>> listDailyRecords(@Param("seasonId") Long seasonId, @Param("userId") Long userId);

    @Select("SELECT record_date as date, COUNT(DISTINCT user_id) as qualifiedUsers " +
            "FROM season_daily_record WHERE season_id = #{seasonId} AND is_qualified = 1 " +
            "GROUP BY record_date ORDER BY record_date")
    List<Map<String, Object>> dailyQualifiedStats(@Param("seasonId") Long seasonId);

    @Select("SELECT COUNT(*) FROM season_daily_record WHERE is_flagged = 1 AND season_id = #{seasonId}")
    Integer countFlaggedRecords(@Param("seasonId") Long seasonId);
}
