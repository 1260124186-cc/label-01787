package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.FamilySharedBook;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FamilySharedBookMapper extends BaseMapper<FamilySharedBook> {

    @Select("SELECT fsb.*, b.title as book_title, b.author as book_author, " +
            "b.book_format as book_format, b.cover_thumbnail as cover_thumbnail, " +
            "u.nickname as shared_by_nickname " +
            "FROM family_shared_book fsb " +
            "LEFT JOIN book b ON fsb.book_id = b.id " +
            "LEFT JOIN user u ON fsb.shared_by = u.id " +
            "WHERE fsb.family_id = #{familyId} " +
            "ORDER BY fsb.shared_at DESC")
    List<Map<String, Object>> selectSharedBooksWithInfo(@Param("familyId") Long familyId);
}
