package com.knowledge.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 请求日志拦截器
 * 记录所有HTTP请求的详细信息
 */
@Slf4j
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUrl = queryString == null ? uri : uri + "?" + queryString;
        String clientIp = getClientIp(request);
        
        log.info("收到请求 - 方法: {}, URL: {}, IP: {}", method, fullUrl, clientIp);
        
        // 如果是POST请求，记录请求体（仅用于调试）
        if ("POST".equals(method) || "PUT".equals(method)) {
            String contentType = request.getContentType();
            log.info("请求Content-Type: {}", contentType);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        int status = response.getStatus();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        if (ex != null) {
            log.error("请求处理异常 - 方法: {}, URL: {}, 状态: {}, 异常: {}", method, uri, status, ex.getMessage());
        } else {
            log.info("请求处理完成 - 方法: {}, URL: {}, 状态: {}", method, uri, status);
        }
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

