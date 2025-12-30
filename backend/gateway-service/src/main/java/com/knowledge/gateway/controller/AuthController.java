package com.knowledge.gateway.controller;

import com.knowledge.api.dto.UserDTO;
import com.knowledge.api.service.UserService;
import com.knowledge.common.result.Result;
import lombok.Data;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @DubboReference(check = false, timeout = 10000)
    private UserService userService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            UserDTO userDTO = userService.login(request.getUsername(), request.getPassword());
            
            // 生成简单的token（实际应该使用JWT）
            String token = "token-" + userDTO.getId() + "-" + System.currentTimeMillis();
            
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUserInfo(userDTO);
            
            return Result.success(response);
        } catch (Exception e) {
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
}

