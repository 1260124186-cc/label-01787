package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.entity.GroupDynamic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface GroupDynamicMapper extends BaseMapper<GroupDynamic> {

    @Select("SELECT gd.*, u.nickname, u.avatar " +
            "FROM group_dynamic gd " +
            "LEFT JOIN user u ON gd.user_id = u.id " +
            "WHERE gd.group_id = #{groupId} " +
            "ORDER BY gd.created_at DESC")
    IPage<Map<String, Object>> selectByGroupIdWithUserInfo(Page<GroupDynamic> page,
                                                            @Param("groupId") Long groupId);

    @Select("SELECT gd.*, u.nickname, u.avatar, g.name as group_name " +
            "FROM group_dynamic gd " +
            "LEFT JOIN user u ON gd.user_id = u.id " +
            "LEFT JOIN book_group g ON gd.group_id = g.id " +
            "WHERE gd.group_id = #{groupId} " +
            "ORDER BY gd.created_at DESC")
    IPage<Map<String, Object>> selectAdminPage(Page<GroupDynamic> page,
                                                @Param("groupId") Long groupId);
}
