package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.ReadingPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReadingPlanMapper extends BaseMapper<ReadingPlan> {

    @Select("SELECT COUNT(*) FROM reading_plan WHERE status = 1")
    Long countActivePlans();

    @Select("SELECT COUNT(*) FROM reading_plan WHERE status = 2")
    Long countCompletedPlans();

    @Select("SELECT COUNT(*) FROM reading_plan WHERE created_at >= #{start} AND created_at < #{end}")
    Long countCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM reading_plan WHERE status = 2 AND completed_at >= #{start} AND completed_at < #{end}")
    Long countCompletedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(DISTINCT user_id) FROM reading_plan WHERE status = 1")
    Long countActivePlanUsers();

    @Select("SELECT COUNT(DISTINCT user_id) FROM reading_plan")
    Long countTotalPlanUsers();

    @Select("SELECT DATE(created_at) as date, COUNT(*) as count FROM reading_plan " +
            "WHERE created_at >= #{start} AND created_at < #{end} " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> dailyCreationStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT DATE(completed_at) as date, COUNT(*) as count FROM reading_plan " +
            "WHERE status = 2 AND completed_at >= #{start} AND completed_at < #{end} " +
            "GROUP BY DATE(completed_at) ORDER BY date")
    List<Map<String, Object>> dailyCompletionStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Update("UPDATE reading_plan SET read_pages = #{readPages}, streak_days = #{streakDays}, " +
            "max_streak_days = GREATEST(max_streak_days, #{streakDays}) WHERE id = #{id}")
    int updateProgress(@Param("id") Long id, @Param("readPages") Integer readPages, @Param("streakDays") Integer streakDays);
}
