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
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        userMapper.insert(user);
        
        UserDTO result = new UserDTO();
        BeanUtils.copyProperties(user, result);
        result.setPermissions(getPermissionsByRole(user.getRole()));
        return result;
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
        UserDTO userDTO = getUserByUsername(username);
        if (userDTO == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 验证密码
        User user = userMapper.selectById(userDTO.getId());
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        return userDTO;
    }
}

