package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.entity.ClassroomSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ClassroomSubmissionMapper extends BaseMapper<ClassroomSubmission> {

    @Select("SELECT cs.*, u.nickname, u.avatar " +
            "FROM classroom_submission cs " +
            "LEFT JOIN user u ON cs.student_id = u.id " +
            "WHERE cs.assignment_id = #{assignmentId} " +
            "ORDER BY cs.submit_at DESC")
    IPage<Map<String, Object>> selectByAssignmentIdWithUserInfo(Page<ClassroomSubmission> page,
                                                                 @Param("assignmentId") Long assignmentId);

    @Select("SELECT cs.* FROM classroom_submission cs " +
            "WHERE cs.assignment_id = #{assignmentId} AND cs.student_id = #{studentId}")
    ClassroomSubmission selectByAssignmentAndStudent(@Param("assignmentId") Long assignmentId,
                                                      @Param("studentId") Long studentId);

    @Select("SELECT COALESCE(AVG(score), 0) FROM classroom_submission " +
            "WHERE assignment_id = #{assignmentId} AND status = 2")
    Double selectAvgScoreByAssignment(@Param("assignmentId") Long assignmentId);

    @Select("SELECT cs.*, u.nickname, u.avatar " +
            "FROM classroom_submission cs " +
            "LEFT JOIN user u ON cs.student_id = u.id " +
            "WHERE cs.assignment_id = #{assignmentId} AND cs.status = 2 " +
            "ORDER BY cs.score DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectTopSubmissions(@Param("assignmentId") Long assignmentId,
                                                     @Param("limit") int limit);
}
