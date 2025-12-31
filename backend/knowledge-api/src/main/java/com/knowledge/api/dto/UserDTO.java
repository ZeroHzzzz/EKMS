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
    private String department;
    private String role;
    private List<String> permissions;
}

