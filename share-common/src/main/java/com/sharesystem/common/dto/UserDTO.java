package com.sharesystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息DTO（不含敏感字段，用于JWT传输）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer role;
    private Integer status;
    private Long storageUsed;
    private Long storageMax;
}
