package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.GroupPlanMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface GroupPlanMemberMapper extends BaseMapper<GroupPlanMember> {

    @Select("SELECT gpm.*, u.nickname, u.avatar " +
            "FROM group_plan_member gpm " +
            "LEFT JOIN user u ON gpm.user_id = u.id " +
            "WHERE gpm.plan_id = #{planId} " +
            "ORDER BY gpm.total_duration DESC")
    List<Map<String, Object>> selectByPlanIdWithUserInfo(@Param("planId") Long planId);

    @Select("SELECT * FROM group_plan_member WHERE plan_id = #{planId} AND user_id = #{userId}")
    GroupPlanMember selectByPlanAndUser(@Param("planId") Long planId, @Param("userId") Long userId);
}
