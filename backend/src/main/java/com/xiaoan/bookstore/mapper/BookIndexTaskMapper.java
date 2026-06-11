package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoan.bookstore.entity.BookIndexTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BookIndexTaskMapper extends BaseMapper<BookIndexTask> {

    @Select("SELECT * FROM book_index_task WHERE status IN (#{status1}, #{status2}) " +
            "ORDER BY priority DESC, created_at ASC LIMIT 1 FOR UPDATE")
    BookIndexTask pickNextTask(@Param("status1") int status1, @Param("status2") int status2);

    @Update("UPDATE book_index_task SET status = #{status}, started_at = #{startedAt} WHERE id = #{id}")
    int updateStatusAndStart(@Param("id") Long id,
                             @Param("status") int status,
                             @Param("startedAt") LocalDateTime startedAt);

    @Update("UPDATE book_index_task SET status = #{status}, indexed_pages = #{indexedPages}, " +
            "finished_at = #{finishedAt}, error_message = #{errorMessage} WHERE id = #{id}")
    int updateStatusAndProgress(@Param("id") Long id,
                                @Param("status") int status,
                                @Param("indexedPages") int indexedPages,
                                @Param("finishedAt") LocalDateTime finishedAt,
                                @Param("errorMessage") String errorMessage);

    @Select("SELECT * FROM book_index_task WHERE status = #{status} ORDER BY created_at DESC")
    List<BookIndexTask> findByStatus(@Param("status") int status);
}
