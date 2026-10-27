package com.example.secondhandplatform.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 状态码：200表示成功，500表示失败
    private String message; // 提示信息
    private T data; // 具体的数据（泛型，什么都能装）

    // 成功时的返回（带数据）
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    // 成功时的返回（带自定义提示，无数据）
    public static <T> Result<T> success(String message) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        return result;
    }

    // 失败时的返回
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
}