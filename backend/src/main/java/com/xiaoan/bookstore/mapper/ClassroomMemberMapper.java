package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.ClassroomMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ClassroomMemberMapper extends BaseMapper<ClassroomMember> {

    @Select("SELECT cm.*, u.nickname, u.avatar " +
            "FROM classroom_member cm " +
            "LEFT JOIN user u ON cm.user_id = u.id " +
            "WHERE cm.classroom_id = #{classroomId} " +
            "ORDER BY cm.role ASC, cm.joined_at ASC")
    List<Map<String, Object>> selectMembersWithUserInfo(@Param("classroomId") Long classroomId);

    @Select("SELECT cm.* FROM classroom_member cm " +
            "WHERE cm.classroom_id = #{classroomId} AND cm.user_id = #{userId}")
    ClassroomMember selectByClassroomAndUser(@Param("classroomId") Long classroomId,
                                              @Param("userId") Long userId);
}
