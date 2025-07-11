package org.cloud.fs.controller;

import org.checkerframework.checker.units.qual.A;
import org.cloud.fs.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

@ControllerAdvice
@ResponseBody
@SuppressWarnings("rawtypes") // 消除ApiResponse的原始类型警告
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServerWebInputException.class)
    public ApiResponse handleServerWebInputException(ServerWebInputException e) {
        logger.error("Invalid request body: {}", e.getMessage());
        return ApiResponse.failure(400, "请求体无法解析");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, WebExchangeBindException.class})
    public ApiResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        logger.error("Invalid request parameters: {}", e.getMessage());
        return ApiResponse.failure(400, "请求参数错误");
    }

    // 捕获所有未明确处理的异常
    @ExceptionHandler(Exception.class)
    public ApiResponse handleException(Exception e) {
        // 打印异常类型、异常消息和堆栈跟踪的首行
        logger.error("Unhandled exception: {} - {} - {}", e.getClass().getName(), e.getMessage(), e.getStackTrace()[0].toString());
        return ApiResponse.failure(500, "内部服务器错误");
    }
}