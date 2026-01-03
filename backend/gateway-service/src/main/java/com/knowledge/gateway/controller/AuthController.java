package com.knowledge.gateway.controller;

import com.knowledge.api.dto.UserDTO;
import com.knowledge.api.service.UserService;
import com.knowledge.common.result.Result;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @DubboReference(check = false, timeout = 10000)
    private UserService userService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("收到登录请求 - 用户名: {}", request.getUsername());
        // 异常由全局异常处理器统一处理，这里不需要try-catch
        UserDTO userDTO = userService.login(request.getUsername(), request.getPassword());
        
        // 生成简单的token（实际应该使用JWT）
        String token = "token-" + userDTO.getId() + "-" + System.currentTimeMillis();
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserInfo(userDTO);
        
        log.info("登录成功 - 用户名: {}, 用户ID: {}", request.getUsername(), userDTO.getId());
        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<RegisterResponse> register(@RequestBody RegisterRequest request) {
        log.info("收到注册请求 - 用户名: {}, 角色: {}, 部门ID: {}", request.getUsername(), request.getRole(), request.getDepartmentId());
        // 异常由全局异常处理器统一处理，这里不需要try-catch
        UserDTO userDTO = userService.register(
            request.getUsername(),
            request.getPassword(),
            request.getRealName(),
            request.getEmail(),
            request.getDepartmentId(),
            request.getRole()
        );
        
        RegisterResponse response = new RegisterResponse();
        response.setUserInfo(userDTO);
        response.setMessage("注册成功");
        
        log.info("注册成功 - 用户名: {}, 用户ID: {}", request.getUsername(), userDTO.getId());
        return Result.success(response);
    }

    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    static class LoginResponse {
        private String token;
        private UserDTO userInfo;
    }

    @Data
    static class RegisterRequest {
        private String username;
        private String password;
        private String realName;
        private String email;
        private Long departmentId;  // 部门ID（系统管理员为null）
        private String role; // ADMIN, EDITOR, USER（系统管理员注册时不需要部门）
    }

    @Data
    static class RegisterResponse {
        private UserDTO userInfo;
        private String message;
    }
}

