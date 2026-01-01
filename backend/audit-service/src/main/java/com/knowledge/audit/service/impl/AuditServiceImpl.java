package com.knowledge.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.api.dto.AuditDTO;
import com.knowledge.api.dto.AuditRecordDTO;
import com.knowledge.api.dto.UserDTO;
import com.knowledge.api.service.AuditService;
import com.knowledge.api.service.KnowledgeService;
import com.knowledge.api.service.UserService;
import com.knowledge.audit.entity.Audit;
import com.knowledge.audit.mapper.AuditMapper;
import com.knowledge.common.constant.Constants;
import org.apache.dubbo.config.annotation.DubboReference;
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
    
    @DubboReference(check = false, timeout = 10000)
    private KnowledgeService knowledgeService;
    
    @DubboReference(check = false, timeout = 10000)
    private UserService userService;

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
        
        // 审核通过后，将知识状态更新为已发布
        if (audit.getKnowledgeId() != null) {
            knowledgeService.publishKnowledge(audit.getKnowledgeId());
        }
        
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
        
        // 审核驳回后，将知识状态更新为草稿（可以重新提交审核）
        if (audit.getKnowledgeId() != null) {
            try {
                // 获取知识信息
                com.knowledge.api.dto.KnowledgeDTO knowledge = knowledgeService.getKnowledgeById(audit.getKnowledgeId());
                if (knowledge != null) {
                    knowledge.setStatus(Constants.FILE_STATUS_DRAFT);
                    knowledge.setUpdateBy("系统");
                    knowledgeService.updateKnowledge(knowledge);
                    log.info("审核驳回后，已将知识状态更新为草稿: knowledgeId={}", audit.getKnowledgeId());
                }
            } catch (Exception e) {
                log.error("审核驳回后更新知识状态失败: knowledgeId={}", audit.getKnowledgeId(), e);
                // 不影响审核驳回的主流程
            }
        }
        
        AuditDTO dto = new AuditDTO();
        BeanUtils.copyProperties(audit, dto);
        return dto;
    }

    @Override
    public List<AuditDTO> getPendingAudits() {
        LambdaQueryWrapper<Audit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Audit::getStatus, Constants.AUDIT_STATUS_PENDING);
        wrapper.orderByDesc(Audit::getCreateTime);

        List<Audit> audits = auditMapper.selectList(wrapper);
        return audits.stream().map(audit -> {
            AuditDTO dto = new AuditDTO();
            BeanUtils.copyProperties(audit, dto);
            dto.setSubmitTime(audit.getCreateTime());
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<AuditDTO> getAllAudits() {
        LambdaQueryWrapper<Audit> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Audit::getCreateTime);

        List<Audit> audits = auditMapper.selectList(wrapper);
        return audits.stream().map(audit -> {
            AuditDTO dto = new AuditDTO();
            BeanUtils.copyProperties(audit, dto);
            dto.setSubmitTime(audit.getCreateTime());
            return dto;
        }).collect(java.util.stream.Collectors.toList());
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
            dto.setAuditTime(audit.getUpdateTime() != null ? audit.getUpdateTime() : audit.getCreateTime());
            
            // 根据审核状态设置操作类型
            if (Constants.AUDIT_STATUS_PENDING.equals(audit.getStatus())) {
                dto.setAction("SUBMIT");
            } else if (Constants.AUDIT_STATUS_APPROVED.equals(audit.getStatus())) {
                dto.setAction("APPROVE");
            } else if (Constants.AUDIT_STATUS_REJECTED.equals(audit.getStatus())) {
                dto.setAction("REJECT");
            }
            
            // 获取审核人姓名
            if (audit.getAuditorId() != null) {
                try {
                    UserDTO auditor = userService.getUserById(audit.getAuditorId());
                    if (auditor != null) {
                        dto.setAuditorName(auditor.getRealName() != null ? auditor.getRealName() : auditor.getUsername());
                    }
                } catch (Exception e) {
                    log.warn("获取审核人信息失败: auditorId={}", audit.getAuditorId(), e);
                }
            }
            
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

