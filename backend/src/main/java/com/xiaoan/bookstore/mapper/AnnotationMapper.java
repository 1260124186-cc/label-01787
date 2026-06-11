package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.Annotation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnnotationMapper extends BaseMapper<Annotation> {

    @Select("SELECT COUNT(*) FROM annotation WHERE user_id = #{userId} AND book_id = #{bookId}")
    Integer countByBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Select("SELECT page_num, COUNT(*) as count FROM annotation WHERE book_id = #{bookId} GROUP BY page_num ORDER BY page_num")
    List<Map<String, Object>> getAnnotationDistributionByBook(@Param("bookId") Long bookId);

    @Select("SELECT page_num, id, user_id, selected_text, content, type, color, created_at FROM annotation WHERE book_id = #{bookId} AND page_num = #{pageNum} ORDER BY created_at DESC")
    List<Map<String, Object>> getAnnotationsByBookAndPage(@Param("bookId") Long bookId, @Param("pageNum") Integer pageNum);
}
