package com.sharesystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    public static <T> Result<T> success() {
        return Result.<T>builder().code(SUCCESS_CODE).message("操作成功").build();
    }

    public static <T> Result<T> success(T data) {
        return Result.<T>builder().code(SUCCESS_CODE).message("操作成功").data(data).build();
    }

    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder().code(SUCCESS_CODE).message(message).data(data).build();
    }

    public static <T> Result<T> error() {
        return Result.<T>builder().code(ERROR_CODE).message("操作失败").build();
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
