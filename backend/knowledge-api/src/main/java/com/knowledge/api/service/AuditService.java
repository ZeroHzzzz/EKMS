package com.knowledge.api.service;

import com.knowledge.api.dto.AuditDTO;
import com.knowledge.api.dto.AuditRecordDTO;

import java.util.List;

public interface AuditService {
    AuditDTO submitForAudit(Long knowledgeId, Long userId);
    AuditDTO approve(Long auditId, Long auditorId, String comment);
    AuditDTO reject(Long auditId, Long auditorId, String comment);
    List<AuditDTO> getPendingAudits();
    List<AuditDTO> getAllAudits(); // 获取所有审核记录
    List<AuditRecordDTO> getAuditHistory(Long knowledgeId);
    List<String> validateContent(String content);
}

