package com.knowledge.gateway.exception;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一处理Controller层抛出的异常，返回友好的错误信息给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理运行时异常（包括RuntimeException）
     * 注意：Dubbo RPC调用时，远程服务抛出的BusinessException会被包装成RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        // 尝试从异常链中提取原始的BusinessException
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof BusinessException) {
                BusinessException be = (BusinessException) cause;
                log.warn("业务异常(从Dubbo传递): {}", be.getMessage());
                return Result.error(be.getCode(), be.getMessage());
            }
            cause = cause.getCause();
        }
        
        // 检查异常消息中是否包含BusinessException的信息（Dubbo包装的情况）
        String message = e.getMessage();
        if (message != null && message.contains("BusinessException")) {
            // 尝试提取原始错误消息
            String extractedMessage = extractBusinessExceptionMessage(message);
            if (extractedMessage != null) {
                log.warn("业务异常(Dubbo包装): {}", extractedMessage);
                return Result.error(400, extractedMessage);
            }
        }
        
        // 真正的运行时异常，记录完整堆栈
        log.error("运行时异常: {}", message, e);
        return Result.error(500, message != null ? message : "系统错误");
    }
    
    /**
     * 从Dubbo包装的异常消息中提取BusinessException的原始消息
     */
    private String extractBusinessExceptionMessage(String fullMessage) {
        if (fullMessage == null) {
            return null;
        }
        // 格式通常是: "com.knowledge.common.exception.BusinessException: 用户名或密码错误"
        int colonIndex = fullMessage.lastIndexOf("BusinessException:");
        if (colonIndex != -1) {
            String afterColon = fullMessage.substring(colonIndex + "BusinessException:".length()).trim();
            // 取第一行作为消息
            int newlineIndex = afterColon.indexOf('\n');
            if (newlineIndex != -1) {
                return afterColon.substring(0, newlineIndex).trim();
            }
            return afterColon;
        }
        return null;
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.error(500, "系统错误，请稍后重试");
    }
}

