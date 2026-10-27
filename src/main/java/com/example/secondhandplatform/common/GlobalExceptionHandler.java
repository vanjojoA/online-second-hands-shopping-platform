package com.example.secondhandplatform.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 捕获所有未知的 Exception（兜底方案）
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        // 在控制台打印真实的报错信息，方便你调试
        e.printStackTrace();

        // 向前端返回标准格式，隐藏真实错误
        return Result.error("服务器开小差了，请稍后再试！");
    }

    // 2. 专门捕获“算术异常”（比如除以 0）
    @ExceptionHandler(ArithmeticException.class)
    public Result<?> handleArithmeticException(ArithmeticException e) {
        e.printStackTrace();
        return Result.error("操作失败：数学计算错误（不能除以0）！");
    }

    // 3. 专门捕获“空指针异常”（新手最容易犯的错）
    @ExceptionHandler(NullPointerException.class)
    public Result<?> handleNullPointerException(NullPointerException e) {
        e.printStackTrace();
        return Result.error("操作失败：系统内部数据缺失（空指针）！");
    }
}