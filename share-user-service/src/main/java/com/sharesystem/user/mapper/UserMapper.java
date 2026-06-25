package com.sharesystem.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sharesystem.common.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsername(String username);

    @Select("SELECT * FROM user WHERE email = #{email}")
    User selectByEmail(String email);

    @Select("SELECT * FROM user ORDER BY create_time DESC")
    List<User> selectAll();

    @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%')")
    List<User> searchUsers(String keyword);

    @Select("SELECT COUNT(*) FROM user WHERE status = 1")
    int countActive();

    @Update("UPDATE user SET storage_used = #{storageUsed} WHERE id = #{id}")
    int updateStorageUsed(@Param("id") Long id, @Param("storageUsed") Long storageUsed);

    @Update("UPDATE user SET storage_max = #{storageMax} WHERE id = #{id}")
    int updateStorageMax(@Param("id") Long id, @Param("storageMax") Long storageMax);

    @Update("UPDATE user SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE user SET last_login_time = NOW() WHERE id = #{id}")
    int updateLoginTime(Long id);
}
