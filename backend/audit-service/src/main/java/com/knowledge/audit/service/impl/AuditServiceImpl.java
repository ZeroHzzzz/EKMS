package com.knowledge.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.api.dto.AuditDTO;
import com.knowledge.api.dto.AuditRecordDTO;
import com.knowledge.api.service.AuditService;
import com.knowledge.audit.entity.Audit;
import com.knowledge.audit.mapper.AuditMapper;
import com.knowledge.common.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@DubboService
public class AuditServiceImpl implements AuditService {

    @Resource
    private AuditMapper auditMapper;

    // 敏感词列表（示例）
    private static final List<String> SENSITIVE_WORDS = Arrays.asList("敏感词1", "敏感词2");

    @Override
    @Transactional
    public AuditDTO submitForAudit(Long knowledgeId, Long userId) {
        Audit audit = new Audit();
        audit.setKnowledgeId(knowledgeId);
        audit.setStatus(Constants.AUDIT_STATUS_PENDING);
        audit.setSubmitUserId(userId);
        audit.setCreateTime(LocalDateTime.now());
        auditMapper.insert(audit);
        
        AuditDTO dto = new AuditDTO();
        BeanUtils.copyProperties(audit, dto);
        return dto;
    }

    @Override
    @Transactional
    public AuditDTO approve(Long auditId, Long auditorId, String comment) {
        Audit audit = auditMapper.selectById(auditId);
        if (audit == null) {
            throw new RuntimeException("审核记录不存在");
        }
        
        audit.setStatus(Constants.AUDIT_STATUS_APPROVED);
        audit.setAuditorId(auditorId);
        audit.setComment(comment);
        audit.setUpdateTime(LocalDateTime.now());
        auditMapper.updateById(audit);
        
        AuditDTO dto = new AuditDTO();
        BeanUtils.copyProperties(audit, dto);
        return dto;
    }

    @Override
    @Transactional
    public AuditDTO reject(Long auditId, Long auditorId, String comment) {
        Audit audit = auditMapper.selectById(auditId);
        if (audit == null) {
            throw new RuntimeException("审核记录不存在");
        }
        
        audit.setStatus(Constants.AUDIT_STATUS_REJECTED);
        audit.setAuditorId(auditorId);
        audit.setComment(comment);
        audit.setUpdateTime(LocalDateTime.now());
        auditMapper.updateById(audit);
        
        AuditDTO dto = new AuditDTO();
        BeanUtils.copyProperties(audit, dto);
        return dto;
    }

    @Override
    public List<AuditRecordDTO> getAuditHistory(Long knowledgeId) {
        LambdaQueryWrapper<Audit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Audit::getKnowledgeId, knowledgeId);
        wrapper.orderByDesc(Audit::getCreateTime);
        
        List<Audit> audits = auditMapper.selectList(wrapper);
        return audits.stream().map(audit -> {
            AuditRecordDTO dto = new AuditRecordDTO();
            BeanUtils.copyProperties(audit, dto);
            dto.setAuditId(audit.getId());
            dto.setAuditorId(audit.getAuditorId());
            dto.setAuditTime(audit.getUpdateTime());
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<String> validateContent(String content) {
        List<String> errors = new ArrayList<>();
        
        // 敏感词检测
        for (String word : SENSITIVE_WORDS) {
            if (content.contains(word)) {
                errors.add("包含敏感词: " + word);
            }
        }
        
        // 错别字检测（简单示例，实际应使用更复杂的算法）
        // 这里只是示例，实际应该使用语言模型或词典
        
        // 格式检查
        if (content.length() < 10) {
            errors.add("内容过短，至少需要10个字符");
        }
        
        return errors;
    }
}

