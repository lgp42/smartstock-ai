package com.smartstock.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS, "success", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(ErrorCode.SUCCESS, "success", null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(ErrorCode.INTERNAL_ERROR, message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
