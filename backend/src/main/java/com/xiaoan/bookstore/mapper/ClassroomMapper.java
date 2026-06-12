package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.entity.Classroom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ClassroomMapper extends BaseMapper<Classroom> {

    @Select("SELECT c.* FROM classroom c " +
            "INNER JOIN classroom_member cm ON c.id = cm.classroom_id " +
            "WHERE cm.user_id = #{userId} " +
            "ORDER BY c.created_at DESC")
    List<Classroom> selectByUserId(@Param("userId") Long userId);

    @Select("<script>SELECT c.* FROM classroom c " +
            "<where> " +
            "<if test='keyword != null and keyword != \"\"'>AND c.name LIKE CONCAT('%', #{keyword}, '%')</if> " +
            "<if test='status != null'>AND c.status = #{status}</if> " +
            "</where> " +
            "ORDER BY c.created_at DESC</script>")
    IPage<Classroom> selectAdminPage(Page<Classroom> page,
                                      @Param("keyword") String keyword,
                                      @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM classroom")
    Long countTotal();

    @Select("SELECT COUNT(*) FROM classroom WHERE status = 0")
    Long countClosed();
}
