package com.knowledge.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.api.dto.UserDTO;
import com.knowledge.api.service.UserService;
import com.knowledge.common.constant.Constants;
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
            return Arrays.asList("ALL");
        } else if (Constants.ROLE_AUDITOR.equals(role)) {
            return Arrays.asList("AUDIT", "VIEW");
        } else {
            return Arrays.asList("VIEW", "UPLOAD");
        }
    }
}

