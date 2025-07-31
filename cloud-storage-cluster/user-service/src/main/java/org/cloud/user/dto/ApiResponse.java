package org.cloud.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Setter
@Getter
public class ApiResponse<T> {
    /*
     * HTTP响应状态码
     */
    @JsonProperty("code")
    private int code;

    /*
     * 响应的消息提示
     */
    @JsonProperty("msg")
    private String msg;

    /*
     * 响应的数据
     */
    @JsonProperty("data")
    private T data; // 返回的数据

    /*
     * 响应的时间戳，格式化后的日期和时间
     */
    @JsonProperty("timestamp")
    private String timestamp;

    // 构造方法
    public ApiResponse() {
        this.code = 200; // 默认成功状态码
        this.msg = "操作成功";
        this.timestamp = getCurrentTimestamp();
    }

    public ApiResponse(int code, String msg) {
        this.code = code;
        this.msg = msg;
        this.timestamp = getCurrentTimestamp();
    }

    public ApiResponse(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = getCurrentTimestamp();
    }

    // 静态方法，方便创建成功和失败的响应
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    public static <T> ApiResponse<T> failure(int code, String msg) {
        return new ApiResponse<>(code, msg);
    }

    public static <T> ApiResponse<T> failure(int code, String msg, T data) {
        return new ApiResponse<>(code, msg, data);
    }

    // 获取当前时间戳的方法
    private String getCurrentTimestamp() {
        // 设置时区为系统默认时区
        ZoneId zoneId = ZoneId.systemDefault();
        // 获取当前时间
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        // 格式化时间戳
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }
}
