package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.GroupDynamicLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupDynamicLikeMapper extends BaseMapper<GroupDynamicLike> {

    @Select("SELECT dynamic_id FROM group_dynamic_like WHERE user_id = #{userId} AND dynamic_id IN (${dynamicIds})")
    List<Long> selectLikedDynamicIds(@Param("userId") Long userId, @Param("dynamicIds") String dynamicIds);
}
