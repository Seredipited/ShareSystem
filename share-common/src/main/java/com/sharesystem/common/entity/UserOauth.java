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
@TableName("user_oauth")
public class UserOauth {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String platform;
    private String openId;
    private String accessToken;
    private String nickname;
    private String avatar;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
