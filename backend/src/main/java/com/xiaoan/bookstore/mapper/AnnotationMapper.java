package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.Annotation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AnnotationMapper extends BaseMapper<Annotation> {

    @Select("SELECT COUNT(*) FROM annotation WHERE user_id = #{userId} AND book_id = #{bookId}")
    Integer countByBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
