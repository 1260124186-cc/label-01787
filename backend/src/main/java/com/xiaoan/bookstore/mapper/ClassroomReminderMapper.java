package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.ClassroomReminder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ClassroomReminderMapper extends BaseMapper<ClassroomReminder> {

    @Select("SELECT * FROM classroom_reminder " +
            "WHERE assignment_id = #{assignmentId} AND student_id = #{studentId} " +
            "ORDER BY created_at DESC LIMIT 1")
    ClassroomReminder selectLatestByAssignmentAndStudent(@Param("assignmentId") Long assignmentId,
                                                          @Param("studentId") Long studentId);

    @Select("SELECT * FROM classroom_reminder " +
            "WHERE assignment_id = #{assignmentId} " +
            "ORDER BY created_at DESC")
    List<ClassroomReminder> selectByAssignmentId(@Param("assignmentId") Long assignmentId);
}
