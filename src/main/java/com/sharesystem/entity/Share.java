package com.sharesystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件分享实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Share {

    private Long id;
    private Long fileId;
    private Long userId;
    /**
     * 分享码
     */
    private String shareCode;
    /**
     * 提取码(可选)
     */
    private String sharePwd;
    /**
     * 过期时间(NULL表示永久有效)
     */
    private LocalDateTime expireTime;
    /**
     * 浏览次数
     */
    private Integer viewCount;
    /**
     * 下载次数
     */
    private Integer downloadCount;
    private LocalDateTime createTime;

    /**
     * 分享是否过期
     */
    public boolean isExpired() {
        if (expireTime == null) return false;
        return LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * 是否有提取码
     */
    public boolean hasPassword() {
        return sharePwd != null && !sharePwd.trim().isEmpty();
    }
}
