package com.knowledge.api.service;

import com.knowledge.api.dto.UserDTO;

public interface UserService {
    UserDTO getUserById(Long userId);
    UserDTO getUserByUsername(String username);
    UserDTO createUser(UserDTO userDTO);
    boolean checkPermission(Long userId, String permission);
    UserDTO login(String username, String password);
}

