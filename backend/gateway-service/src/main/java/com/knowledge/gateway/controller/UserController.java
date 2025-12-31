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
@RequestMapping("/api/user")
public class UserController {

    @DubboReference(check = false, timeout = 10000)
    private UserService userService;

    @GetMapping("/{id}")
    public Result<UserDTO> getUser(@PathVariable Long id) {
        try {
            UserDTO userDTO = userService.getUserById(id);
            if (userDTO == null) {
                return Result.error("用户不存在");
            }
            // 清除密码字段
            userDTO.setPassword(null);
            return Result.success(userDTO);
        } catch (Exception e) {
            log.error("获取用户信息失败 - 用户ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<UserDTO> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        log.info("更新用户信息 - 用户ID: {}", id);
        try {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(id);
            userDTO.setRealName(request.getRealName());
            userDTO.setEmail(request.getEmail());
            userDTO.setDepartment(request.getDepartment());
            
            UserDTO result = userService.updateUser(userDTO);
            result.setPassword(null); // 清除密码字段
            
            log.info("用户信息更新成功 - 用户ID: {}", id);
            return Result.success(result);
        } catch (Exception e) {
            log.error("更新用户信息失败 - 用户ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/password")
    public Result<Void> updatePassword(@PathVariable Long id, @RequestBody UpdatePasswordRequest request) {
        log.info("修改密码 - 用户ID: {}", id);
        try {
            userService.updatePassword(id, request.getOldPassword(), request.getNewPassword());
            log.info("密码修改成功 - 用户ID: {}", id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("修改密码失败 - 用户ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Data
    static class UpdateUserRequest {
        private String realName;
        private String email;
        private String department;
    }

    @Data
    static class UpdatePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}

