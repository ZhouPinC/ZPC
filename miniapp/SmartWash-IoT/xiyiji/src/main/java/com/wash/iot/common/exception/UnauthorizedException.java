package com.wash.iot.common.exception;

/**
 * 未授权异常
 */
public class UnauthorizedException extends RuntimeException {
    
    private int code = 401;
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, int code) {
        super(message);
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
}
