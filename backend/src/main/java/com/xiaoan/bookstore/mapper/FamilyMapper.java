package com.xiaoan.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.entity.Family;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FamilyMapper extends BaseMapper<Family> {

    @Select("SELECT f.* FROM family f " +
            "INNER JOIN family_member fm ON f.id = fm.family_id " +
            "WHERE fm.user_id = #{userId} AND f.status = 1 " +
            "ORDER BY f.created_at DESC")
    List<Family> selectFamiliesByUserId(@Param("userId") Long userId);

    @Select("<script>SELECT f.* FROM family f " +
            "<where> f.status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>AND f.name LIKE CONCAT('%', #{keyword}, '%')</if> " +
            "</where> " +
            "ORDER BY f.created_at DESC</script>")
    IPage<Family> selectAdminPage(Page<Family> page, @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM family WHERE status = 1")
    Long countActiveFamilies();
}
