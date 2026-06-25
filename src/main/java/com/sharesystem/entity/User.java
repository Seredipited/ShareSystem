package com.sharesystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String avatar;
    private String nickname;
    /**
     * 角色: 0-普通用户, 1-管理员
     */
    private Integer role;
    /**
     * 已用存储空间(字节)
     */
    private Long storageUsed;
    /**
     * 最大存储空间(字节), 默认1GB
     */
    private Long storageMax;
    /**
     * 状态: 0-禁用, 1-正常
     */
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public boolean isAdmin() {
        return role != null && role == 1;
    }

    public boolean isActive() {
        return status != null && status == 1;
    }
}
