package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.InkStroke;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface InkStrokeMapper extends BaseMapper<InkStroke> {

    @Select("SELECT * FROM ink_stroke WHERE user_id = #{userId} AND book_id = #{bookId} AND page_num = #{pageNum} ORDER BY created_at ASC")
    List<InkStroke> selectByPage(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("pageNum") Integer pageNum);

    @Select("SELECT * FROM ink_stroke WHERE user_id = #{userId} AND book_id = #{bookId} ORDER BY page_num ASC, created_at ASC")
    List<InkStroke> selectByBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Select("SELECT page_num, COUNT(*) as stroke_count FROM ink_stroke WHERE user_id = #{userId} AND book_id = #{bookId} GROUP BY page_num ORDER BY page_num")
    List<java.util.Map<String, Object>> selectPageStats(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Delete("DELETE FROM ink_stroke WHERE user_id = #{userId} AND book_id = #{bookId} AND stroke_id = #{strokeId}")
    int deleteByStrokeId(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("strokeId") String strokeId);

    @Select("SELECT COUNT(*) FROM ink_stroke WHERE user_id = #{userId} AND book_id = #{bookId} AND page_num = #{pageNum}")
    Integer countByPage(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("pageNum") Integer pageNum);
}
