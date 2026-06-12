package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.SeasonParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface SeasonParticipantMapper extends BaseMapper<SeasonParticipant> {

    @Select("SELECT * FROM season_participant WHERE season_id = #{seasonId} AND user_id = #{userId}")
    SeasonParticipant findBySeasonAndUser(@Param("seasonId") Long seasonId, @Param("userId") Long userId);

    @Select("SELECT sp.season_id, rs.title, rs.season_type, rs.start_date, rs.end_date, " +
            "sp.status, sp.qualified_days, sp.total_duration, sp.streak_days, sp.badge_awarded " +
            "FROM season_participant sp " +
            "LEFT JOIN reading_season rs ON sp.season_id = rs.id " +
            "WHERE sp.user_id = #{userId} AND sp.status IN (1, 2) " +
            "ORDER BY rs.start_date DESC")
    List<Map<String, Object>> findActiveByUser(@Param("userId") Long userId);

    @Select("SELECT sp.season_id, rs.title, rs.season_type, rs.start_date, rs.end_date, " +
            "sp.status, sp.qualified_days, sp.total_duration, sp.streak_days, sp.badge_awarded, sp.completed_at " +
            "FROM season_participant sp " +
            "LEFT JOIN reading_season rs ON sp.season_id = rs.id " +
            "WHERE sp.user_id = #{userId} " +
            "ORDER BY rs.start_date DESC LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> findByUserPaged(@Param("userId") Long userId,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM season_participant WHERE user_id = #{userId}")
    Integer countByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM season_participant WHERE season_id = #{seasonId} AND status = 2")
    Integer countCompletedBySeason(@Param("seasonId") Long seasonId);

    @Select("SELECT COUNT(*) FROM season_participant WHERE season_id = #{seasonId} AND status = 4")
    Integer countCheatDisqualified(@Param("seasonId") Long seasonId);

    @Update("UPDATE season_participant SET streak_days = #{streakDays}, max_streak_days = #{maxStreakDays}, " +
            "qualified_days = #{qualifiedDays}, total_duration = #{totalDuration}, " +
            "total_books = #{totalBooks}, status = #{status} " +
            "WHERE season_id = #{seasonId} AND user_id = #{userId}")
    int updateProgress(@Param("seasonId") Long seasonId, @Param("userId") Long userId,
                       @Param("streakDays") int streakDays, @Param("maxStreakDays") int maxStreakDays,
                       @Param("qualifiedDays") int qualifiedDays, @Param("totalDuration") long totalDuration,
                       @Param("totalBooks") int totalBooks, @Param("status") int status);

    @Select("SELECT COALESCE(SUM(duration), 0) FROM season_daily_record " +
            "WHERE season_id = #{seasonId} AND user_id = #{userId} AND is_qualified = 1")
    Long sumQualifiedDuration(@Param("seasonId") Long seasonId, @Param("userId") Long userId);
}
