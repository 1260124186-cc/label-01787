package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.ClassroomAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ClassroomAssignmentMapper extends BaseMapper<ClassroomAssignment> {

    @Select("SELECT * FROM classroom_assignment " +
            "WHERE classroom_id = #{classroomId} " +
            "ORDER BY created_at DESC")
    List<ClassroomAssignment> selectByClassroomId(@Param("classroomId") Long classroomId);

    @Select("SELECT * FROM classroom_assignment " +
            "WHERE classroom_id = #{classroomId} AND status = #{status} " +
            "ORDER BY deadline ASC")
    List<ClassroomAssignment> selectByClassroomIdAndStatus(@Param("classroomId") Long classroomId,
                                                            @Param("status") Integer status);
}
