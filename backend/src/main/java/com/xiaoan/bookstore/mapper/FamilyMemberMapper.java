package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.FamilyMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface FamilyMemberMapper extends BaseMapper<FamilyMember> {

    @Select("SELECT fm.*, u.nickname, u.avatar " +
            "FROM family_member fm " +
            "LEFT JOIN user u ON fm.user_id = u.id " +
            "WHERE fm.family_id = #{familyId} " +
            "ORDER BY fm.role ASC, fm.joined_at ASC")
    List<Map<String, Object>> selectMembersWithUserInfo(@Param("familyId") Long familyId);

    @Select("SELECT fm.user_id, u.nickname, u.avatar, fm.role, fm.nickname as family_nickname, fm.joined_at, " +
            "COALESCE(SUM(rr.duration), 0) as total_duration " +
            "FROM family_member fm " +
            "LEFT JOIN user u ON fm.user_id = u.id " +
            "LEFT JOIN reading_record rr ON fm.user_id = rr.user_id " +
            "AND rr.start_time >= #{start} AND rr.start_time < #{end} " +
            "WHERE fm.family_id = #{familyId} " +
            "GROUP BY fm.user_id " +
            "ORDER BY total_duration DESC")
    List<Map<String, Object>> selectMembersWithReadingStats(@Param("familyId") Long familyId,
                                                             @Param("start") LocalDateTime start,
                                                             @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM family_member WHERE family_id = #{familyId}")
    Integer countMembersByFamilyId(@Param("familyId") Long familyId);
}
