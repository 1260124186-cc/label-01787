package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.ConvertTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ConvertTaskMapper extends BaseMapper<ConvertTask> {

    @Select("SELECT * FROM convert_task WHERE status IN (0, 1) ORDER BY priority DESC, created_at ASC LIMIT #{limit}")
    List<ConvertTask> pickPendingTasks(@Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM convert_task WHERE user_id = #{userId} AND status = 0")
    int countPendingByUserId(@Param("userId") Long userId);
}
