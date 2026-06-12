package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.FocusSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface FocusSessionMapper extends BaseMapper<FocusSession> {

    @Select("SELECT COALESCE(SUM(actual_duration), 0) FROM focus_session " +
            "WHERE user_id = #{userId} AND status = 1 AND started_at >= #{start} AND started_at < #{end}")
    Long sumActualDuration(@Param("userId") Long userId,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM focus_session " +
            "WHERE user_id = #{userId} AND status = 1 AND started_at >= #{start} AND started_at < #{end}")
    Integer countCompletedSessions(@Param("userId") Long userId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Select("SELECT DATE(started_at) as date, COUNT(*) as count, SUM(actual_duration) as totalDuration " +
            "FROM focus_session " +
            "WHERE user_id = #{userId} AND status = 1 AND started_at >= #{start} AND started_at < #{end} " +
            "GROUP BY DATE(started_at) ORDER BY date")
    List<Map<String, Object>> dailyStats(@Param("userId") Long userId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Select("SELECT tag, COUNT(*) as count, SUM(actual_duration) as totalDuration " +
            "FROM focus_session " +
            "WHERE user_id = #{userId} AND status = 1 AND started_at >= #{start} AND started_at < #{end} " +
            "GROUP BY tag ORDER BY totalDuration DESC")
    List<Map<String, Object>> tagStats(@Param("userId") Long userId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(MAX(pomodoro_index), 0) FROM focus_session " +
            "WHERE user_id = #{userId} AND DATE(started_at) = CURDATE() AND status = 1")
    Integer todayMaxPomodoroIndex(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM focus_session " +
            "WHERE user_id = #{userId} AND status = 0")
    Integer countActiveSessions(@Param("userId") Long userId);
}
