package com.sharesystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 第三方登录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOauth {

    private Long id;
    private Long userId;
    /**
     * 平台: qq, wechat
     */
    private String platform;
    /**
     * 第三方OpenID
     */
    private String openId;
    private String accessToken;
    private String nickname;
    private String avatar;
    private LocalDateTime createTime;
}
