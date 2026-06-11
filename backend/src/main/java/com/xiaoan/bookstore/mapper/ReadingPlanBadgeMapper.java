package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.ReadingPlanBadge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReadingPlanBadgeMapper extends BaseMapper<ReadingPlanBadge> {

    @Select("SELECT * FROM reading_plan_badge WHERE user_id = #{userId} ORDER BY earned_at DESC")
    List<ReadingPlanBadge> listByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM reading_plan_badge WHERE user_id = #{userId} AND badge_type = #{badgeType}")
    int countByUserAndType(@Param("userId") Long userId, @Param("badgeType") String badgeType);
}
