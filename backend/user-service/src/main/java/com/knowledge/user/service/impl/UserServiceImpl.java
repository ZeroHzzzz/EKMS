package com.knowledge.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.api.dto.UserDTO;
import com.knowledge.api.service.UserService;
import com.knowledge.common.constant.Constants;
import com.knowledge.common.util.PasswordUtil;
import com.knowledge.user.entity.User;
import com.knowledge.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@DubboService
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public UserDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
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
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
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
            throw new RuntimeException("用户名已存在");
        }
        
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        
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
        
        UserDTO result = new UserDTO();
        BeanUtils.copyProperties(user, result);
        result.setPermissions(getPermissionsByRole(user.getRole()));
        return result;
    }

    @Override
    public UserDTO register(String username, String password, String realName, String email, String department, String role) {
        log.info("注册请求 - 用户名: {}, 角色: {}", username, role);
        
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User existingUser = userMapper.selectOne(wrapper);
        if (existingUser != null) {
            log.warn("注册失败 - 用户名已存在: {}", username);
            throw new RuntimeException("用户名已存在");
        }
        
        // 验证角色
        if (role == null || role.isEmpty()) {
            role = Constants.ROLE_USER; // 默认角色为普通用户
        }
        if (!Constants.ROLE_ADMIN.equals(role) && 
            !Constants.ROLE_EDITOR.equals(role) && 
            !Constants.ROLE_USER.equals(role)) {
            throw new RuntimeException("无效的用户角色");
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(password)); // 加密密码
        user.setRealName(realName);
        user.setEmail(email);
        user.setDepartment(department);
        user.setRole(role);
        
        userMapper.insert(user);
        log.info("注册成功 - 用户名: {}, 角色: {}", username, role);
        
        // 转换为UserDTO（不包含密码）
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        userDTO.setPassword(null); // 清除密码字段
        userDTO.setPermissions(getPermissionsByRole(user.getRole()));
        return userDTO;
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
            throw new RuntimeException("用户名或密码错误");
        }
        
        log.info("找到用户 - ID: {}, 用户名: {}", user.getId(), user.getUsername());
        
        // 验证密码
        String storedPasswordHash = user.getPassword();
        // 输出密码hash的前缀和长度，用于调试（不输出完整hash）
        String hashPrefix = storedPasswordHash != null && storedPasswordHash.length() > 10 
            ? storedPasswordHash.substring(0, 10) + "..." 
            : storedPasswordHash;
        log.info("存储的密码hash信息 - 长度: {}, 前缀: {}", 
            storedPasswordHash != null ? storedPasswordHash.length() : 0, hashPrefix);
        log.info("输入的密码长度: {}", password != null ? password.length() : 0);
        
        boolean passwordMatches = PasswordUtil.matches(password, storedPasswordHash);
        
        log.info("密码验证结果 - 匹配: {}", passwordMatches);
        
        if (!passwordMatches) {
            log.warn("登录失败 - 密码不匹配，用户名: {}", username);
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 转换为UserDTO
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        userDTO.setPermissions(getPermissionsByRole(user.getRole()));
        
        log.info("登录成功 - 用户名: {}, 角色: {}", username, user.getRole());
        
        return userDTO;
    }
}

