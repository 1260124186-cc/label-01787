package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.SeasonBadge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SeasonBadgeMapper extends BaseMapper<SeasonBadge> {

    @Select("SELECT * FROM season_badge WHERE user_id = #{userId} ORDER BY earned_at DESC")
    List<SeasonBadge> listByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM season_badge WHERE user_id = #{userId} AND season_id = #{seasonId} ORDER BY earned_at DESC")
    List<SeasonBadge> listByUserAndSeason(@Param("userId") Long userId, @Param("seasonId") Long seasonId);

    @Select("SELECT COUNT(*) FROM season_badge WHERE user_id = #{userId} AND season_id = #{seasonId} AND badge_type = #{badgeType}")
    Integer countByUserSeasonType(@Param("userId") Long userId, @Param("seasonId") Long seasonId, @Param("badgeType") String badgeType);

    @Select("SELECT badge_type, COUNT(*) as count FROM season_badge WHERE season_id = #{seasonId} GROUP BY badge_type")
    List<java.util.Map<String, Object>> badgeStatsBySeason(@Param("seasonId") Long seasonId);
}
