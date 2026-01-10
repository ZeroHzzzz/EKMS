package com.knowledge.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_permission")
public class KnowledgePermission extends BaseEntity {
    private Long knowledgeId;
    private Long userId;
    
    /**
     * 权限类型: VIEW | EDIT
     */
    private String permissionType;
    
    public Long getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getPermissionType() { return permissionType; }
    public void setPermissionType(String permissionType) { this.permissionType = permissionType; }
}
