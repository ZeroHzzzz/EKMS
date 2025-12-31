package com.knowledge.api.service;

import com.knowledge.api.dto.UserDTO;

public interface UserService {
    UserDTO getUserById(Long userId);
    UserDTO getUserByUsername(String username);
    UserDTO createUser(UserDTO userDTO);
    UserDTO register(String username, String password, String realName, String email, String department, String role);
    UserDTO updateUser(UserDTO userDTO);
    void updatePassword(Long userId, String oldPassword, String newPassword);
    boolean checkPermission(Long userId, String permission);
    UserDTO login(String username, String password);
}

