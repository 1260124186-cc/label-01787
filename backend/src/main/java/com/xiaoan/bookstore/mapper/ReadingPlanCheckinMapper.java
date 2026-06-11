package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.ReadingPlanCheckin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReadingPlanCheckinMapper extends BaseMapper<ReadingPlanCheckin> {

    @Select("SELECT * FROM reading_plan_checkin WHERE plan_id = #{planId} AND checkin_date = #{date}")
    ReadingPlanCheckin findByPlanAndDate(@Param("planId") Long planId, @Param("date") LocalDate date);

    @Select("SELECT * FROM reading_plan_checkin WHERE plan_id = #{planId} ORDER BY checkin_date DESC")
    List<ReadingPlanCheckin> listByPlanId(@Param("planId") Long planId);

    @Select("SELECT checkin_date FROM reading_plan_checkin WHERE plan_id = #{planId} ORDER BY checkin_date")
    List<LocalDate> listCheckinDates(@Param("planId") Long planId);

    @Select("SELECT SUM(duration) FROM reading_plan_checkin WHERE plan_id = #{planId} AND checkin_date >= #{start} AND checkin_date <= #{end}")
    Integer sumDurationBetween(@Param("planId") Long planId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT SUM(pages_read) FROM reading_plan_checkin WHERE plan_id = #{planId}")
    Integer sumPagesRead(@Param("planId") Long planId);

    @Select("SELECT COALESCE(SUM(duration), 0) FROM reading_plan_checkin WHERE user_id = #{userId} AND checkin_date = #{date}")
    Integer sumUserDailyDuration(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT COUNT(DISTINCT user_id) FROM reading_plan_checkin WHERE checkin_date >= #{start} AND checkin_date <= #{end}")
    Long countCheckinUsersBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COUNT(*) FROM reading_plan_checkin WHERE checkin_date = #{date}")
    Long countCheckinsOnDate(@Param("date") LocalDate date);

    @Select("SELECT checkin_date as date, COUNT(DISTINCT user_id) as userCount " +
            "FROM reading_plan_checkin WHERE checkin_date >= #{start} AND checkin_date <= #{end} " +
            "GROUP BY checkin_date ORDER BY date")
    List<Map<String, Object>> dailyCheckinStats(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
