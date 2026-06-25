package com.sharesystem.mapper;

import com.sharesystem.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户Mapper
 */
public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectById(Long id);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsername(String username);

    @Select("SELECT * FROM user WHERE email = #{email}")
    User selectByEmail(String email);

    @Select("SELECT * FROM user WHERE status = 1 ORDER BY create_time DESC")
    List<User> selectAll();

    @Select("SELECT * FROM user WHERE role = #{role} AND status = 1 ORDER BY create_time DESC")
    List<User> selectByRole(Integer role);

    @Select("SELECT COUNT(*) FROM user WHERE status = 1")
    int countActive();

    @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%')")
    List<User> searchUsers(String keyword);

    @Insert("INSERT INTO user(username, password, email, phone, avatar, nickname, role, storage_used, storage_max, status) " +
            "VALUES(#{username}, #{password}, #{email}, #{phone}, #{avatar}, #{nickname}, #{role}, #{storageUsed}, #{storageMax}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET " +
            "password = #{password}, email = #{email}, phone = #{phone}, " +
            "avatar = #{avatar}, nickname = #{nickname}, " +
            "storage_used = #{storageUsed}, storage_max = #{storageMax}, " +
            "status = #{status}, last_login_time = #{lastLoginTime} " +
            "WHERE id = #{id}")
    int update(User user);

    @Update("UPDATE user SET storage_used = #{storageUsed} WHERE id = #{id}")
    int updateStorageUsed(@Param("id") Long id, @Param("storageUsed") Long storageUsed);

    @Update("UPDATE user SET storage_max = #{storageMax} WHERE id = #{id}")
    int updateStorageMax(@Param("id") Long id, @Param("storageMax") Long storageMax);

    @Update("UPDATE user SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE user SET last_login_time = NOW() WHERE id = #{id}")
    int updateLoginTime(Long id);

    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(Long id);
}
