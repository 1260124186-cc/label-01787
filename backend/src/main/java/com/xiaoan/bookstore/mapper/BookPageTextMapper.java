package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.BookPageText;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookPageTextMapper extends BaseMapper<BookPageText> {

    @Select("SELECT * FROM book_page_text WHERE user_id = #{userId} AND MATCH(page_text) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) " +
            "ORDER BY MATCH(page_text) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) DESC " +
            "LIMIT #{offset}, #{limit}")
    List<BookPageText> searchByFullText(@Param("userId") Long userId,
                                        @Param("keyword") String keyword,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    @Select("SELECT * FROM book_page_text WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "AND MATCH(page_text) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) " +
            "ORDER BY MATCH(page_text) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) DESC " +
            "LIMIT #{offset}, #{limit}")
    List<BookPageText> searchByBookAndFullText(@Param("userId") Long userId,
                                               @Param("bookId") Long bookId,
                                               @Param("keyword") String keyword,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM book_page_text WHERE user_id = #{userId} AND MATCH(page_text) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE)")
    int countByFullText(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM book_page_text WHERE user_id = #{userId} AND book_id = #{bookId} " +
            "AND MATCH(page_text) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE)")
    int countByBookAndFullText(@Param("userId") Long userId,
                               @Param("bookId") Long bookId,
                               @Param("keyword") String keyword);
}
