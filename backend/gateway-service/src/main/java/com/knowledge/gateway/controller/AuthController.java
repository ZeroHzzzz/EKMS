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
        try {
            UserDTO userDTO = userService.login(request.getUsername(), request.getPassword());
            
            // 生成简单的token（实际应该使用JWT）
            String token = "token-" + userDTO.getId() + "-" + System.currentTimeMillis();
            
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUserInfo(userDTO);
            
            log.info("登录成功 - 用户名: {}, 用户ID: {}", request.getUsername(), userDTO.getId());
            return Result.success(response);
        } catch (Exception e) {
            log.error("登录失败 - 用户名: {}, 错误: {}", request.getUsername(), e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<RegisterResponse> register(@RequestBody RegisterRequest request) {
        log.info("收到注册请求 - 用户名: {}, 角色: {}", request.getUsername(), request.getRole());
        try {
            UserDTO userDTO = userService.register(
                request.getUsername(),
                request.getPassword(),
                request.getRealName(),
                request.getEmail(),
                request.getDepartment(),
                request.getRole()
            );
            
            RegisterResponse response = new RegisterResponse();
            response.setUserInfo(userDTO);
            response.setMessage("注册成功");
            
            log.info("注册成功 - 用户名: {}, 用户ID: {}", request.getUsername(), userDTO.getId());
            return Result.success(response);
        } catch (Exception e) {
            log.error("注册失败 - 用户名: {}, 错误: {}", request.getUsername(), e.getMessage(), e);
            return Result.error(e.getMessage());
        }
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
        private String department;
        private String role; // ADMIN, EDITOR, USER
    }

    @Data
    static class RegisterResponse {
        private UserDTO userInfo;
        private String message;
    }
}

