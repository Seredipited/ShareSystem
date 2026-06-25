package com.sharesystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微服务之间 RPC 调用的统一响应包装
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class R<T> {
    private Integer code;
    private String msg;
    private T data;

    public static <T> R<T> ok(T data) {
        return R.<T>builder().code(200).msg("success").data(data).build();
    }

    public static <T> R<T> ok() {
        return R.<T>builder().code(200).msg("success").build();
    }

    public static <T> R<T> fail(String msg) {
        return R.<T>builder().code(500).msg(msg).build();
    }

    public static <T> R<T> fail(Integer code, String msg) {
        return R.<T>builder().code(code).msg(msg).build();
    }

    public boolean success() {
        return code != null && code == 200;
    }
}
