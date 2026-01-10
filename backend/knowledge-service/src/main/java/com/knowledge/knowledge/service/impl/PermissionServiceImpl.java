package com.knowledge.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.api.dto.KnowledgePermissionDTO;
import com.knowledge.api.dto.UserDTO;
import com.knowledge.api.service.PermissionService;
import com.knowledge.api.service.UserService;
import com.knowledge.common.constant.Constants;
import com.knowledge.knowledge.entity.Knowledge;
import com.knowledge.knowledge.entity.KnowledgePermission;
import com.knowledge.knowledge.mapper.KnowledgeMapper;
import com.knowledge.knowledge.mapper.KnowledgePermissionMapper;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@DubboService
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private KnowledgePermissionMapper permissionMapper;
    
    @Resource
    private KnowledgeMapper knowledgeMapper;

    @DubboReference(check = false)
    private UserService userService;

    @Override
    @Transactional
    public boolean setPermission(Long knowledgeId, Long userId, String permissionType) {
        // 先检查是否已有权限
        LambdaQueryWrapper<KnowledgePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgePermission::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgePermission::getUserId, userId);
        
        KnowledgePermission existing = permissionMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setPermissionType(permissionType);
            existing.setUpdateTime(LocalDateTime.now());
            return permissionMapper.updateById(existing) > 0;
        } else {
            KnowledgePermission perm = new KnowledgePermission();
            perm.setKnowledgeId(knowledgeId);
            perm.setUserId(userId);
            perm.setPermissionType(permissionType);
            perm.setCreateTime(LocalDateTime.now());
            perm.setUpdateTime(LocalDateTime.now());
            return permissionMapper.insert(perm) > 0;
        }
    }

    @Override
    @Transactional
    public boolean removePermission(Long knowledgeId, Long userId) {
        LambdaQueryWrapper<KnowledgePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgePermission::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgePermission::getUserId, userId);
        return permissionMapper.delete(wrapper) > 0;
    }

    @Override
    public List<KnowledgePermissionDTO> getPermissions(Long knowledgeId) {
        LambdaQueryWrapper<KnowledgePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgePermission::getKnowledgeId, knowledgeId);
        List<KnowledgePermission> list = permissionMapper.selectList(wrapper);
        
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        
        return list.stream().map(p -> {
            KnowledgePermissionDTO dto = new KnowledgePermissionDTO();
            BeanUtils.copyProperties(p, dto);
            
            // 填充用户信息
            try {
                UserDTO user = userService.getUserById(p.getUserId());
                if (user != null) {
                    dto.setUsername(user.getUsername());
                    dto.setRealName(user.getRealName());
                }
            } catch (Exception e) {
                // 忽略用户服务调用失败
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean hasPermission(Long knowledgeId, Long userId, String requiredType) {
        if (userId == null) return false;
        
        // 1. 检查是否存在该文档
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null) return false;
        
        // 2. 如果是 Admin，直接通过
        try {
            UserDTO user = userService.getUserById(userId);
            if (user != null && Constants.ROLE_ADMIN.equals(user.getRole())) {
                return true;
            }
        } catch (Exception e) {}
        
        // 3. 如果是作者/创建者，直接通过
        try {
            UserDTO user = userService.getUserById(userId);
            if (user != null && user.getUsername().equals(knowledge.getCreateBy())) {
                return true;
            }
        } catch (Exception e) {}
        
        // 4. 公开文档检查
        // VIEW 权限：如果不私有，则所有人可查看
        if ("VIEW".equals(requiredType) && !Boolean.TRUE.equals(knowledge.getIsPrivate())) {
            return true;
        }
        
        // 5. 检查特定权限表
        LambdaQueryWrapper<KnowledgePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgePermission::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgePermission::getUserId, userId);
        KnowledgePermission perm = permissionMapper.selectOne(wrapper);
        
        if (perm == null) return false;
        
        // EDIT需要Explicit EDIT Permission
        if ("EDIT".equals(requiredType)) {
            return "EDIT".equals(perm.getPermissionType());
        }
        
        // VIEW需要任意权限
        return true;
    }
}
