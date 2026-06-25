package com.sharesystem.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("share")
public class Share {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;
    private Long userId;
    private String shareCode;
    private String sharePwd;
    private LocalDateTime expireTime;
    private Integer viewCount;
    private Integer downloadCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public boolean isExpired() {
        if (expireTime == null) return false;
        return LocalDateTime.now().isAfter(expireTime);
    }

    public boolean hasPassword() {
        return sharePwd != null && !sharePwd.trim().isEmpty();
    }
}
