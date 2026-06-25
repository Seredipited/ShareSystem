package com.sharesystem.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sharesystem.common.entity.UserOauth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface UserOauthMapper extends BaseMapper<UserOauth> {

    @Select("SELECT * FROM user_oauth WHERE platform = #{platform} AND open_id = #{openId}")
    UserOauth selectByPlatformAndOpenId(@Param("platform") String platform, @Param("openId") String openId);

    @Select("SELECT * FROM user_oauth WHERE user_id = #{userId}")
    UserOauth selectByUserId(Long userId);

    @Delete("DELETE FROM user_oauth WHERE user_id = #{userId} AND platform = #{platform}")
    int deleteByUserIdAndPlatform(@Param("userId") Long userId, @Param("platform") String platform);
}
