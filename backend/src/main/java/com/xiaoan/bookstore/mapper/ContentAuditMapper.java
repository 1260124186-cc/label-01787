package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.ContentAudit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ContentAuditMapper extends BaseMapper<ContentAudit> {

    @Select("SELECT COUNT(*) FROM content_audit WHERE result = #{result} AND created_at >= #{startTime} AND created_at <= #{endTime}")
    Long countByResultAndTimeRange(@Param("result") int result, @Param("startTime") java.time.LocalDateTime startTime, @Param("endTime") java.time.LocalDateTime endTime);
}
