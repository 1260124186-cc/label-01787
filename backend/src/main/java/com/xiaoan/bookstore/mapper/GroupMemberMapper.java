package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    @Select("SELECT gm.*, u.nickname, u.avatar " +
            "FROM group_member gm " +
            "LEFT JOIN user u ON gm.user_id = u.id " +
            "WHERE gm.group_id = #{groupId} " +
            "ORDER BY gm.role ASC, gm.joined_at ASC")
    List<Map<String, Object>> selectMembersWithUserInfo(@Param("groupId") Long groupId);

    @Select("SELECT gm.user_id, u.nickname, u.avatar, gm.role, gm.reading_public, gm.joined_at, " +
            "COALESCE(SUM(rr.duration), 0) as total_duration " +
            "FROM group_member gm " +
            "LEFT JOIN user u ON gm.user_id = u.id " +
            "LEFT JOIN reading_record rr ON gm.user_id = rr.user_id " +
            "AND rr.start_time >= #{weekStart} AND rr.start_time < #{weekEnd} " +
            "WHERE gm.group_id = #{groupId} " +
            "GROUP BY gm.user_id " +
            "ORDER BY total_duration DESC")
    List<Map<String, Object>> selectWeekRank(@Param("groupId") Long groupId,
                                              @Param("weekStart") LocalDateTime weekStart,
                                              @Param("weekEnd") LocalDateTime weekEnd);

    @Select("SELECT gm.user_id, u.nickname, u.avatar, gm.reading_public, " +
            "COALESCE(SUM(rr.duration), 0) as total_duration, " +
            "b.title as book_title " +
            "FROM group_member gm " +
            "LEFT JOIN user u ON gm.user_id = u.id " +
            "LEFT JOIN reading_record rr ON gm.user_id = rr.user_id " +
            "AND rr.start_time >= #{weekStart} AND rr.start_time < #{weekEnd} " +
            "LEFT JOIN book b ON rr.book_id = b.id " +
            "WHERE gm.group_id = #{groupId} " +
            "GROUP BY gm.user_id " +
            "ORDER BY total_duration DESC")
    List<Map<String, Object>> selectWeekRankWithBooks(@Param("groupId") Long groupId,
                                                       @Param("weekStart") LocalDateTime weekStart,
                                                       @Param("weekEnd") LocalDateTime weekEnd);

    @Select("SELECT COUNT(*) FROM group_member WHERE group_id = #{groupId}")
    Integer countMembersByGroupId(@Param("groupId") Long groupId);
}
