package com.knowledge.common.exception;

/**
 * 业务异常
 * 用于处理业务逻辑错误，不应该直接暴露给前端
 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    private Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public Integer getCode() {
        return code;
    }
}

