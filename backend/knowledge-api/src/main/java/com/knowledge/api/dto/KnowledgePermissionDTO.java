package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class KnowledgePermissionDTO implements Serializable {
    private Long id;
    private Long knowledgeId;
    private Long userId;
    private String username;    // Display name
    private String realName;    // Display name
    private String permissionType; // VIEW | EDIT
}
