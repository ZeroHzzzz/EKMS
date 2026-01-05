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
        // 获取知识当前版本
        Long version = null;
        try {
            com.knowledge.api.dto.KnowledgeDTO knowledge = knowledgeService.getKnowledgeById(knowledgeId);
            if (knowledge != null) {
                version = knowledge.getVersion();
            }
        } catch (Exception e) {
            log.warn("获取知识版本失败: knowledgeId={}", knowledgeId, e);
        }
        return submitForAudit(knowledgeId, version, userId);
    }
    
    @Override
    @Transactional
    public AuditDTO submitForAudit(Long knowledgeId, Long version, Long userId) {
        // 检查是否已存在同一知识同一版本的待审核记录
        LambdaQueryWrapper<Audit> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Audit::getKnowledgeId, knowledgeId);
        existWrapper.eq(Audit::getStatus, Constants.AUDIT_STATUS_PENDING);
        if (version != null) {
            existWrapper.eq(Audit::getVersion, version);
        }
        Audit existingAudit = auditMapper.selectOne(existWrapper);
        if (existingAudit != null) {
            log.info("已存在该版本的待审核记录: knowledgeId={}, version={}, auditId={}", 
                    knowledgeId, version, existingAudit.getId());
            AuditDTO dto = new AuditDTO();
            BeanUtils.copyProperties(existingAudit, dto);
            dto.setSubmitTime(existingAudit.getCreateTime());
            return dto;
        }
        
        Audit audit = new Audit();
        audit.setKnowledgeId(knowledgeId);
        audit.setVersion(version);
        audit.setStatus(Constants.AUDIT_STATUS_PENDING);
        audit.setSubmitUserId(userId);
        audit.setCreateTime(LocalDateTime.now());
        auditMapper.insert(audit);
        
        log.info("创建审核记录: knowledgeId={}, version={}, auditId={}", knowledgeId, version, audit.getId());
        
        AuditDTO dto = new AuditDTO();
        BeanUtils.copyProperties(audit, dto);
        dto.setSubmitTime(audit.getCreateTime());
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
        
        // 审核通过后，发布对应版本
        if (audit.getKnowledgeId() != null) {
            if (audit.getVersion() != null) {
                // 有版本号时，发布指定版本
                knowledgeService.publishVersion(audit.getKnowledgeId(), audit.getVersion());
                log.info("审核通过，发布指定版本: knowledgeId={}, version={}", audit.getKnowledgeId(), audit.getVersion());
            } else {
                // 兼容旧数据，没有版本号时发布当前版本
                knowledgeService.publishKnowledge(audit.getKnowledgeId());
                log.info("审核通过，发布当前版本: knowledgeId={}", audit.getKnowledgeId());
            }
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
        
        // 审核驳回后，更新版本状态
        if (audit.getKnowledgeId() != null) {
            try {
                com.knowledge.api.dto.KnowledgeDTO knowledge = knowledgeService.getKnowledgeById(audit.getKnowledgeId());
                if (knowledge != null) {
                    // 将对应版本的状态更新为已驳回（通过knowledgeService）
                    if (audit.getVersion() != null) {
                        knowledgeService.rejectVersion(audit.getKnowledgeId(), audit.getVersion());
                        log.info("审核驳回后，已将版本状态更新为已驳回: knowledgeId={}, version={}", 
                                audit.getKnowledgeId(), audit.getVersion());
                    }
                    
                    // 判断是否需要更新knowledge表的状态
                    // 只有在没有已发布版本的情况下，才将知识整体状态设为已驳回
                    if (knowledge.getPublishedVersion() == null) {
                        // 没有已发布版本，将整体状态设为已驳回
                        knowledge.setStatus(Constants.FILE_STATUS_REJECTED);
                        knowledge.setHasDraft(false);
                        knowledge.setUpdateBy("系统");
                        knowledgeService.updateKnowledgeStatus(knowledge.getId(), Constants.FILE_STATUS_REJECTED, false);
                        log.info("审核驳回后，知识无已发布版本，将整体状态更新为已驳回: knowledgeId={}", audit.getKnowledgeId());
                    } else {
                        // 有已发布版本，只清除草稿标记，保留已发布状态
                        knowledgeService.updateKnowledgeStatus(knowledge.getId(), Constants.FILE_STATUS_APPROVED, false);
                        log.info("审核驳回后，知识有已发布版本，保留发布状态，清除草稿标记: knowledgeId={}, publishedVersion={}", 
                                audit.getKnowledgeId(), knowledge.getPublishedVersion());
                    }
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
            
            // 获取知识信息用于展示
            try {
                if (audit.getKnowledgeId() != null) {
                    com.knowledge.api.dto.KnowledgeDTO knowledge = knowledgeService.getKnowledgeById(audit.getKnowledgeId());
                    if (knowledge != null) {
                        dto.setKnowledgeTitle(knowledge.getTitle());
                        dto.setFileId(knowledge.getFileId());
                    }
                }
            } catch (Exception e) {
                log.warn("获取知识信息失败: knowledgeId={}", audit.getKnowledgeId(), e);
            }
            
            // 获取提交人姓名
            try {
                if (audit.getSubmitUserId() != null) {
                    UserDTO user = userService.getUserById(audit.getSubmitUserId());
                    if (user != null) {
                        dto.setSubmitUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                    }
                }
            } catch (Exception e) {
                log.warn("获取提交人信息失败: userId={}", audit.getSubmitUserId(), e);
            }
            
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
            
            // 获取知识信息用于展示
            try {
                if (audit.getKnowledgeId() != null) {
                    com.knowledge.api.dto.KnowledgeDTO knowledge = knowledgeService.getKnowledgeById(audit.getKnowledgeId());
                    if (knowledge != null) {
                        dto.setKnowledgeTitle(knowledge.getTitle());
                        dto.setFileId(knowledge.getFileId());
                    }
                }
            } catch (Exception e) {
                log.warn("获取知识信息失败: knowledgeId={}", audit.getKnowledgeId(), e);
            }
            
            // 获取提交人姓名
            try {
                if (audit.getSubmitUserId() != null) {
                    UserDTO user = userService.getUserById(audit.getSubmitUserId());
                    if (user != null) {
                        dto.setSubmitUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                    }
                }
            } catch (Exception e) {
                log.warn("获取提交人信息失败: userId={}", audit.getSubmitUserId(), e);
            }
            
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
            dto.setKnowledgeId(audit.getKnowledgeId());
            dto.setVersion(audit.getVersion());  // 设置版本号
            dto.setAuditorId(audit.getAuditorId());
            dto.setStatus(audit.getStatus());
            dto.setAuditTime(audit.getUpdateTime() != null ? audit.getUpdateTime() : audit.getCreateTime());
            dto.setSubmitTime(audit.getCreateTime());
            
            // 根据审核状态设置操作类型
            if (Constants.AUDIT_STATUS_PENDING.equals(audit.getStatus())) {
                dto.setAction("SUBMIT");
            } else if (Constants.AUDIT_STATUS_APPROVED.equals(audit.getStatus())) {
                dto.setAction("APPROVE");
            } else if (Constants.AUDIT_STATUS_REJECTED.equals(audit.getStatus())) {
                dto.setAction("REJECT");
            }
            
            // 获取提交人姓名
            if (audit.getSubmitUserId() != null) {
                try {
                    UserDTO submitter = userService.getUserById(audit.getSubmitUserId());
                    if (submitter != null) {
                        dto.setSubmitUserName(submitter.getRealName() != null ? submitter.getRealName() : submitter.getUsername());
                    }
                } catch (Exception e) {
                    log.warn("获取提交人信息失败: submitUserId={}", audit.getSubmitUserId(), e);
                }
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

    @Override
    @Transactional
    public void deleteByKnowledgeId(Long knowledgeId) {
        if (knowledgeId == null) return;
        
        LambdaQueryWrapper<Audit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Audit::getKnowledgeId, knowledgeId);
        auditMapper.delete(wrapper);
        log.info("删除知识关联的审核记录: knowledgeId={}", knowledgeId);
    }

    @Override
    @Transactional
    public void deleteByKnowledgeIdAndVersionGt(Long knowledgeId, Long version) {
        if (knowledgeId == null || version == null) return;

        LambdaQueryWrapper<Audit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Audit::getKnowledgeId, knowledgeId);
        wrapper.gt(Audit::getVersion, version);
        int count = auditMapper.delete(wrapper);
        log.info("删除指定版本之后的审核记录: knowledgeId={}, version>{}, count={}", knowledgeId, version, count);
    }
}

