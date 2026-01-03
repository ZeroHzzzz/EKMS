package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private Long departmentId;  // 部门ID，系统管理员为null
    private String department;  // 部门名称（用于显示）
    private String role;
    private List<String> permissions;
}

