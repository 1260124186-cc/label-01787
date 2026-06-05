package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.FileDownloadLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FileDownloadLogMapper extends BaseMapper<FileDownloadLog> {

    @Select("SELECT COUNT(*) FROM file_download_log " +
            "WHERE file_token = #{fileToken} " +
            "AND created_at > #{since}")
    int countByTokenSince(String fileToken, java.time.LocalDateTime since);

    @Select("SELECT COUNT(*) FROM file_download_log " +
            "WHERE user_id = #{userId} AND user_type = #{userType} " +
            "AND created_at > #{since}")
    int countByUserSince(Long userId, Integer userType, java.time.LocalDateTime since);
}
