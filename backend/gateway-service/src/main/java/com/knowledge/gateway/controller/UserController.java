package com.knowledge.gateway.controller;

import com.knowledge.api.dto.DepartmentDTO;
import com.knowledge.api.dto.UserDTO;
import com.knowledge.api.service.DepartmentService;
import com.knowledge.api.service.UserService;
import com.knowledge.common.result.Result;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @DubboReference(check = false, timeout = 10000)
    private UserService userService;

    @DubboReference(check = false, timeout = 10000)
    private DepartmentService departmentService;

    @GetMapping("/list")
    public Result<List<UserDTO>> getAllUsers() {
        try {
            List<UserDTO> users = userService.getAllUsers();
            return Result.success(users);
        } catch (Exception e) {
            log.error("获取用户列表失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

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

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        log.info("删除用户 - 用户ID: {}", id);
        try {
            userService.deleteUser(id);
            log.info("用户删除成功 - 用户ID: {}", id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除用户失败 - 用户ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/role")
    public Result<Void> updateUserRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        log.info("更新用户角色 - 用户ID: {}, 新角色: {}", id, request.getRole());
        try {
            userService.updateUserRole(id, request.getRole());
            log.info("用户角色更新成功 - 用户ID: {}", id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("更新用户角色失败 - 用户ID: {}, 错误: {}", id, e.getMessage(), e);
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
            userDTO.setDepartmentId(request.getDepartmentId());
            userDTO.setRole(request.getRole());  // 允许更新角色
            
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

    @PostMapping
    public Result<UserDTO> createUser(@RequestBody CreateUserRequest request) {
        log.info("创建用户 - 用户名: {}", request.getUsername());
        try {
            UserDTO userDTO = new UserDTO();
            userDTO.setUsername(request.getUsername());
            userDTO.setPassword(request.getPassword());
            userDTO.setRealName(request.getRealName());
            userDTO.setEmail(request.getEmail());
            userDTO.setDepartmentId(request.getDepartmentId());
            userDTO.setRole(request.getRole());
            
            UserDTO result = userService.createUser(userDTO);
            result.setPassword(null); // 清除密码字段
            
            log.info("用户创建成功 - 用户ID: {}", result.getId());
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建用户失败 - 用户名: {}, 错误: {}", request.getUsername(), e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/departments")
    @Deprecated
    public Result<List<String>> getAllDepartments() {
        // 此接口已废弃，请使用 /api/department 接口
        try {
            List<DepartmentDTO> departments = departmentService.getAllDepartments();
            List<String> departmentNames = departments.stream()
                .map(DepartmentDTO::getName)
                .collect(java.util.stream.Collectors.toList());
            return Result.success(departmentNames);
        } catch (Exception e) {
            log.error("获取部门列表失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Data
    static class CreateUserRequest {
        private String username;
        private String password;
        private String realName;
        private String email;
        private Long departmentId;
        private String role;
    }

    @Data
    static class UpdateUserRequest {
        private String realName;
        private String email;
        private Long departmentId;  // 部门ID
        private String role;  // 角色
    }

    @Data
    static class UpdatePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }

    @Data
    static class UpdateRoleRequest {
        private String role;
    }
}

