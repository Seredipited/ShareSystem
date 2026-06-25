package com.sharesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginDTO {

    private String username;
    private String password;
    /**
     * 登录类型: password-密码登录, qq-QQ快捷登录
     */
    private String type;
    /**
     * QQ登录时的code
     */
    private String code;
}
