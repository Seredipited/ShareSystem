package com.sharesystem.mapper;

import com.sharesystem.entity.UserOauth;
import org.apache.ibatis.annotations.*;

/**
 * 第三方登录Mapper
 */
public interface UserOauthMapper {

    @Select("SELECT * FROM user_oauth WHERE id = #{id}")
    UserOauth selectById(Long id);

    @Select("SELECT * FROM user_oauth WHERE platform = #{platform} AND open_id = #{openId}")
    UserOauth selectByPlatformAndOpenId(@Param("platform") String platform, @Param("openId") String openId);

    @Select("SELECT * FROM user_oauth WHERE user_id = #{userId}")
    UserOauth selectByUserId(Long userId);

    @Insert("INSERT INTO user_oauth(user_id, platform, open_id, access_token, nickname, avatar) " +
            "VALUES(#{userId}, #{platform}, #{openId}, #{accessToken}, #{nickname}, #{avatar})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserOauth oauth);

    @Update("UPDATE user_oauth SET access_token = #{accessToken}, nickname = #{nickname}, avatar = #{avatar} WHERE id = #{id}")
    int update(UserOauth oauth);

    @Delete("DELETE FROM user_oauth WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM user_oauth WHERE user_id = #{userId} AND platform = #{platform}")
    int deleteByUserIdAndPlatform(@Param("userId") Long userId, @Param("platform") String platform);
}
