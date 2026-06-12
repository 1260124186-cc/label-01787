package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.SeasonCheatDetection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface SeasonCheatDetectionMapper extends BaseMapper<SeasonCheatDetection> {

    @Select("SELECT scd.*, u.nickname, u.avatar_url, rs.title as seasonTitle " +
            "FROM season_cheat_detection scd " +
            "LEFT JOIN user u ON scd.user_id = u.id " +
            "LEFT JOIN reading_season rs ON scd.season_id = rs.id " +
            "WHERE scd.status = #{status} " +
            "ORDER BY scd.severity DESC, scd.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> findByStatusPaged(@Param("status") int status,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM season_cheat_detection WHERE status = #{status}")
    Long countByStatus(@Param("status") int status);

    @Select("SELECT detection_type, COUNT(*) as count FROM season_cheat_detection " +
            "WHERE season_id = #{seasonId} GROUP BY detection_type")
    List<Map<String, Object>> detectionTypeStats(@Param("seasonId") Long seasonId);

    @Select("SELECT COUNT(*) FROM season_cheat_detection WHERE user_id = #{userId} AND season_id = #{seasonId} AND status = 1")
    Integer countConfirmedCheat(@Param("userId") Long userId, @Param("seasonId") Long seasonId);

    @Select("SELECT * FROM season_cheat_detection WHERE season_id = #{seasonId} AND user_id = #{userId} AND detection_date = #{date}")
    List<SeasonCheatDetection> findBySeasonUserDate(@Param("seasonId") Long seasonId,
                                                     @Param("userId") Long userId,
                                                     @Param("date") LocalDate date);
}
