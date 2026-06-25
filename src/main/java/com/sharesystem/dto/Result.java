package com.sharesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    private static final int SUCCESS_CODE = 200;
    private static final int ERROR_CODE = 500;
    private static final String SUCCESS_MSG = "操作成功";
    private static final String ERROR_MSG = "操作失败";

    public static <T> Result<T> success() {
        return Result.<T>builder().code(SUCCESS_CODE).message(SUCCESS_MSG).build();
    }

    public static <T> Result<T> success(T data) {
        return Result.<T>builder().code(SUCCESS_CODE).message(SUCCESS_MSG).data(data).build();
    }

    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder().code(SUCCESS_CODE).message(message).data(data).build();
    }

    public static <T> Result<T> error() {
        return Result.<T>builder().code(ERROR_CODE).message(ERROR_MSG).build();
    }

    public static <T> Result<T> error(String message) {
        return Result.<T>builder().code(ERROR_CODE).message(message).build();
    }

    public static <T> Result<T> error(Integer code, String message) {
        return Result.<T>builder().code(code).message(message).build();
    }

    public boolean isSuccess() {
        return code != null && code == SUCCESS_CODE;
    }
}
