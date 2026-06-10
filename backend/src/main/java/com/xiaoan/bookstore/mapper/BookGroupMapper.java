package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.entity.BookGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BookGroupMapper extends BaseMapper<BookGroup> {

    @Select("SELECT g.* FROM book_group g " +
            "INNER JOIN group_member gm ON g.id = gm.group_id " +
            "WHERE gm.user_id = #{userId} " +
            "ORDER BY g.created_at DESC")
    List<BookGroup> selectGroupsByUserId(@Param("userId") Long userId);

    @Select("<script>SELECT g.* FROM book_group g " +
            "<where> " +
            "<if test='keyword != null and keyword != \"\"'>AND g.name LIKE CONCAT('%', #{keyword}, '%')</if> " +
            "<if test='status != null'>AND g.status = #{status}</if> " +
            "</where> " +
            "ORDER BY g.created_at DESC</script>")
    IPage<BookGroup> selectAdminPage(Page<BookGroup> page,
                                      @Param("keyword") String keyword,
                                      @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM book_group")
    Long countTotalGroups();

    @Select("SELECT COUNT(*) FROM book_group WHERE status = 0")
    Long countBannedGroups();
}
