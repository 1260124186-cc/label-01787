package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.ReadingSeason;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReadingSeasonMapper extends BaseMapper<ReadingSeason> {

    @Select("SELECT COUNT(*) FROM season_participant WHERE season_id = #{seasonId} AND status IN (1, 2)")
    Integer countActiveParticipants(@Param("seasonId") Long seasonId);

    @Select("SELECT * FROM reading_season WHERE status IN (1, 2) AND #{date} BETWEEN start_date AND end_date ORDER BY start_date DESC")
    List<ReadingSeason> findActiveByDate(@Param("date") LocalDate date);

    @Select("SELECT * FROM reading_season WHERE status = 1 AND #{date} BETWEEN signup_start AND signup_end ORDER BY signup_end ASC")
    List<ReadingSeason> findSignupAvailable(@Param("date") LocalDate date);

    @Select("SELECT sp.*, u.nickname, u.avatar_url " +
            "FROM season_participant sp " +
            "LEFT JOIN user u ON sp.user_id = u.id " +
            "WHERE sp.season_id = #{seasonId} AND sp.status IN (1, 2) " +
            "ORDER BY sp.qualified_days DESC, sp.total_duration DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> leaderboard(@Param("seasonId") Long seasonId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    @Select("SELECT COUNT(*) + 1 FROM season_participant " +
            "WHERE season_id = #{seasonId} AND status IN (1, 2) " +
            "AND (qualified_days > #{qualifiedDays} OR (qualified_days = #{qualifiedDays} AND total_duration > #{totalDuration}))")
    Integer getUserRank(@Param("seasonId") Long seasonId,
                        @Param("qualifiedDays") int qualifiedDays,
                        @Param("totalDuration") long totalDuration);

    @Select("SELECT COUNT(*) FROM reading_season WHERE status != 4")
    Long countTotalSeasons();

    @Select("SELECT COUNT(*) FROM reading_season WHERE status = 2")
    Long countActiveSeasons();

    @Select("SELECT COUNT(DISTINCT user_id) FROM season_participant WHERE status IN (1, 2)")
    Long countTotalParticipants();

    @Select("SELECT DATE(created_at) as date, COUNT(*) as count FROM reading_season WHERE status != 4 AND created_at >= #{start} AND created_at < #{end} GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> dailyCreationStats(@Param("start") java.time.LocalDateTime start,
                                                  @Param("end") java.time.LocalDateTime end);

    @Select("SELECT season_type, COUNT(*) as count FROM reading_season WHERE status != 4 GROUP BY season_type")
    List<Map<String, Object>> seasonTypeStats();

    @Update("UPDATE reading_season SET status = #{status} WHERE id = #{seasonId}")
    int updateStatus(@Param("seasonId") Long seasonId, @Param("status") int status);
}
