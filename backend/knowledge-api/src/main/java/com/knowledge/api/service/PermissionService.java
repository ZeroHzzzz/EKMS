package com.knowledge.api.service;

import com.knowledge.api.dto.KnowledgePermissionDTO;
import java.util.List;

public interface PermissionService {
    /**
     * Set permission for a user on a knowledge document
     * @param knowledgeId knowledge ID
     * @param userId user ID
     * @param info permission type (VIEW | EDIT)
     */
    boolean setPermission(Long knowledgeId, Long userId, String permissionType);
    
    /**
     * Remove permission for a user
     */
    boolean removePermission(Long knowledgeId, Long userId);
    
    /**
     * Get all permissions for a knowledge document
     */
    List<KnowledgePermissionDTO> getPermissions(Long knowledgeId);
    
    /**
     * Check if user has permission
     */
    boolean hasPermission(Long knowledgeId, Long userId, String requiredType);
}
