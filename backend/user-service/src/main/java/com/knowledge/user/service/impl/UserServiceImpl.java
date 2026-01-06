package com.knowledge.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.api.dto.UserDTO;
import com.knowledge.api.service.UserService;
import com.knowledge.common.constant.Constants;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.util.PasswordUtil;
import com.knowledge.common.util.UserValidationUtil;
import com.knowledge.user.entity.User;
import com.knowledge.user.util.UserDTOUtil;
import com.knowledge.user.mapper.DepartmentMapper;
import com.knowledge.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@DubboService
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public UserDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        UserDTO userDTO = UserDTOUtil.toDTO(user, departmentMapper);
        userDTO.setPermissions(getPermissionsByRole(user.getRole()));
        return userDTO;
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return null;
        }
        UserDTO userDTO = UserDTOUtil.toDTO(user, departmentMapper);
        userDTO.setPermissions(getPermissionsByRole(user.getRole()));
        return userDTO;
    }

    @Override
    public UserDTO createUser(UserDTO userDTO) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, userDTO.getUsername());
        User existingUser = userMapper.selectOne(wrapper);
        if (existingUser != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        
        // 验证角色
        if (userDTO.getRole() == null || !UserValidationUtil.isValidRole(userDTO.getRole())) {
            throw new BusinessException(400, "无效的用户角色");
        }
        
        // 验证部门
        UserValidationUtil.validateDepartment(userDTO.getRole(), userDTO.getDepartmentId());
        
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        
        // 设置部门ID（使用工具类统一处理）
        UserDTOUtil.setDepartmentId(user, userDTO.getRole(), userDTO.getDepartmentId());
        
        // 如果密码不为空且不是已加密的BCrypt格式，则加密密码
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            // 检查密码是否是BCrypt格式（以$2a$或$2b$开头且长度为60）
            String password = userDTO.getPassword();
            if (!password.startsWith("$2a$") && !password.startsWith("$2b$") && password.length() != 60) {
                // 密码是明文，需要加密
                user.setPassword(PasswordUtil.encode(password));
            } else {
                // 密码已经是加密格式，直接使用
                user.setPassword(password);
            }
        }
        
        userMapper.insert(user);
        
        UserDTO result = UserDTOUtil.toDTO(user, departmentMapper);
        result.setPermissions(getPermissionsByRole(user.getRole()));
        return result;
    }

    @Override
    public UserDTO register(String username, String password, String realName, String email, Long departmentId, String role) {
        log.info("注册请求 - 用户名: {}, 角色: {}, 部门ID: {}", username, role, departmentId);
        
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User existingUser = userMapper.selectOne(wrapper);
        if (existingUser != null) {
            log.warn("注册失败 - 用户名已存在: {}", username);
            throw new BusinessException(400, "用户名已存在");
        }
        
        // 验证角色
        if (role == null || role.isEmpty()) {
            role = Constants.ROLE_USER; // 默认角色为普通用户
        }
        UserValidationUtil.validateRegisterRole(role);
        
        // 验证部门：系统管理员不需要部门，其他角色必须选择部门
        UserValidationUtil.validateDepartment(role, departmentId);
        
        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(password)); // 加密密码
        user.setRealName(realName);
        user.setEmail(email);
        user.setRole(role);
        
        // 设置部门ID（使用工具类统一处理）
        UserDTOUtil.setDepartmentId(user, role, departmentId);
        
        userMapper.insert(user);
        log.info("注册成功 - 用户名: {}, 角色: {}, 部门ID: {}", username, role, user.getDepartmentId());
        
        // 转换为UserDTO（不包含密码）
        UserDTO userDTO = UserDTOUtil.toDTO(user, departmentMapper);
        userDTO.setPermissions(getPermissionsByRole(user.getRole()));
        return userDTO;
    }

    @Override
    @Transactional
    public UserDTO updateUser(UserDTO userDTO) {
        log.info("更新用户信息 - 用户ID: {}", userDTO.getId());
        
        User user = userMapper.selectById(userDTO.getId());
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        
        // 只更新允许的字段，不允许修改用户名
        user.setRealName(userDTO.getRealName());
        user.setEmail(userDTO.getEmail());
        
        // 如果提供了角色且角色发生变化，允许更新角色（仅系统管理员可以修改角色）
        String targetRole = userDTO.getRole() != null && !userDTO.getRole().isEmpty() 
            ? userDTO.getRole() 
            : user.getRole();
        
        // 验证角色
        if (!UserValidationUtil.isValidRole(targetRole)) {
            throw new BusinessException(400, "无效的用户角色");
        }
        
        // 验证并设置部门ID（使用工具类统一处理）
        UserValidationUtil.validateDepartment(targetRole, userDTO.getDepartmentId());
        UserDTOUtil.setDepartmentId(user, targetRole, userDTO.getDepartmentId());
        
        // 更新角色
        user.setRole(targetRole);
        
        userMapper.updateById(user);
        log.info("用户信息更新成功 - 用户ID: {}", userDTO.getId());
        
        // 返回更新后的用户信息
        UserDTO result = UserDTOUtil.toDTO(user, departmentMapper);
        result.setPermissions(getPermissionsByRole(user.getRole()));
        return result;
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        log.info("修改密码 - 用户ID: {}", userId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        
        // 验证原密码
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            log.warn("密码修改失败 - 原密码错误，用户ID: {}", userId);
            throw new BusinessException(400, "原密码错误");
        }
        
        // 加密新密码
        user.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(user);
        
        log.info("密码修改成功 - 用户ID: {}", userId);
    }

    @Override
    public boolean checkPermission(Long userId, String permission) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        List<String> permissions = getPermissionsByRole(user.getRole());
        return permissions.contains(permission);
    }

    private List<String> getPermissionsByRole(String role) {
        if (Constants.ROLE_ADMIN.equals(role)) {
            // 总管理员：拥有所有权限，包括审核、用户管理等
            return Arrays.asList(
                Constants.PERMISSION_ALL,
                Constants.PERMISSION_VIEW,
                Constants.PERMISSION_UPLOAD,
                Constants.PERMISSION_EDIT,
                Constants.PERMISSION_DELETE,
                Constants.PERMISSION_DOWNLOAD,
                Constants.PERMISSION_BATCH_UPLOAD,
                Constants.PERMISSION_BATCH_DOWNLOAD,
                Constants.PERMISSION_AUDIT,
                Constants.PERMISSION_SUBMIT_AUDIT,
                Constants.PERMISSION_VIEW_AUDIT,
                Constants.PERMISSION_MANAGE_USER,
                Constants.PERMISSION_MANAGE_ROLE,
                Constants.PERMISSION_MANAGE_CONFIG,
                Constants.PERMISSION_MANAGE_STRUCTURE,
                Constants.PERMISSION_VIEW_STATISTICS,
                Constants.PERMISSION_EXPORT_DATA
            );
        } else if (Constants.ROLE_EDITOR.equals(role)) {
            // 各部门知识管理员：上传、编辑、提交审核、管理知识结构
            return Arrays.asList(
                Constants.PERMISSION_VIEW,
                Constants.PERMISSION_UPLOAD,
                Constants.PERMISSION_EDIT,
                Constants.PERMISSION_DELETE,
                Constants.PERMISSION_DOWNLOAD,
                Constants.PERMISSION_BATCH_UPLOAD,
                Constants.PERMISSION_BATCH_DOWNLOAD,
                Constants.PERMISSION_SUBMIT_AUDIT,
                Constants.PERMISSION_VIEW_AUDIT,
                Constants.PERMISSION_MANAGE_STRUCTURE,
                Constants.PERMISSION_VIEW_STATISTICS
            );
        } else {
            // 普通员工：查看、搜索、下载
            return Arrays.asList(
                Constants.PERMISSION_VIEW,
                Constants.PERMISSION_DOWNLOAD,
                Constants.PERMISSION_VIEW_STATISTICS
            );
        }
    }

    @Override
    public UserDTO login(String username, String password) {
        log.info("登录请求 - 用户名: {}", username);
        
        // 直接查询User实体，避免重复查询
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            log.warn("登录失败 - 用户不存在: {}", username);
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        log.info("找到用户 - ID: {}, 用户名: {}", user.getId(), user.getUsername());
        
        // 验证密码
        String storedPasswordHash = user.getPassword();
        boolean passwordMatches = PasswordUtil.matches(password, storedPasswordHash);
        
        log.info("密码验证结果 - 匹配: {}", passwordMatches);
        
        if (!passwordMatches) {
            log.warn("登录失败 - 密码不匹配，用户名: {}", username);
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 转换为UserDTO
        UserDTO userDTO = UserDTOUtil.toDTO(user, departmentMapper);
        userDTO.setPermissions(getPermissionsByRole(user.getRole()));
        
        log.info("登录成功 - 用户名: {}, 角色: {}", username, user.getRole());
        
        return userDTO;
    }

    @Override
    public List<String> getAllDepartments() {
        // 此方法已废弃，应该使用DepartmentService.getAllDepartments()
        // 保留此方法以保持向后兼容，返回空列表
        log.warn("getAllDepartments()方法已废弃，请使用DepartmentService.getAllDepartments()");
        return List.of();
    }

    @Override
    public List<UserDTO> getAllUsers() {
        log.info("获取所有用户列表");
        List<User> users = userMapper.selectList(null);
        return users.stream()
            .map(user -> {
                UserDTO dto = UserDTOUtil.toDTO(user, departmentMapper);
                dto.setPassword(null); // 不返回密码
                dto.setPermissions(getPermissionsByRole(user.getRole()));
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("删除用户 - 用户ID: {}", userId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        userMapper.deleteById(userId);
        log.info("用户删除成功 - 用户ID: {}", userId);
    }

    @Override
    @Transactional
    public void updateUserRole(Long userId, String role) {
        log.info("更新用户角色 - 用户ID: {}, 新角色: {}", userId, role);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        
        // 验证角色
        if (!UserValidationUtil.isValidRole(role)) {
            throw new BusinessException(400, "无效的用户角色");
        }
        
        // 如果角色从非管理员变为管理员，清空部门ID
        if (Constants.ROLE_ADMIN.equals(role)) {
            user.setDepartmentId(null);
        }
        
        user.setRole(role);
        userMapper.updateById(user);
        
        log.info("用户角色更新成功 - 用户ID: {}, 新角色: {}", userId, role);
    }
    @Override
    @Transactional
    public void addPoints(Long userId, Integer points, String type, String description) {
        log.info("Add points - userId: {}, points: {}, type: {}", userId, points, type);
        User user = userMapper.selectById(userId);
        if (user == null) return;
        
        int currentPoints = user.getPoints() == null ? 0 : user.getPoints();
        user.setPoints(currentPoints + points);
        userMapper.updateById(user);
        
        // Note: Ideally insert into user_point_log table here.
        // For now we just update the user points tally.
    }

    @Override
    public List<UserDTO> getLeaderboard(int limit) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(User::getPoints);
        wrapper.last("LIMIT " + limit);
        
        List<User> users = userMapper.selectList(wrapper);
        return users.stream().map(user -> {
            UserDTO dto = UserDTOUtil.toDTO(user, departmentMapper);
            dto.setPermissions(null); // No need for permissions in public leaderboard
            dto.setPassword(null);
            return dto;
        }).collect(Collectors.toList());
    }
}

