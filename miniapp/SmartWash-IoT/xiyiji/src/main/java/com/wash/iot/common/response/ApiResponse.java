package com.wash.iot.common.response;

import lombok.Data;

/**
 * 统一API响应格式
 */
@Data
public class ApiResponse<T> {
    
    private int code;
    private String message;
    private T data;
    private long timestamp;
    
    private ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, "success", null);
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(-1, message, null);
    }
    
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
