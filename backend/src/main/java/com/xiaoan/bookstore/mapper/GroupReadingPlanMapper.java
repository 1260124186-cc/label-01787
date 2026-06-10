package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.GroupReadingPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupReadingPlanMapper extends BaseMapper<GroupReadingPlan> {

    @Select("SELECT * FROM group_reading_plan WHERE group_id = #{groupId} ORDER BY created_at DESC")
    List<GroupReadingPlan> selectByGroupId(@Param("groupId") Long groupId);
}
