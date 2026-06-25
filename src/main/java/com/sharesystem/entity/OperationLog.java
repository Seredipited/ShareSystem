package com.sharesystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationLog {

    private Long id;
    private Long userId;
    private String username;
    /**
     * 操作类型
     */
    private String operation;
    /**
     * 操作对象
     */
    private String target;
    /**
     * 详细信息
     */
    private String detail;
    private String ip;
    private LocalDateTime createTime;
}
