package com.knowledge.audit.listener;

import com.knowledge.api.service.KnowledgeService;
import com.knowledge.common.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Flowable Listener: Triggered when audit is REJECTED.
 * Updates Knowledge status via remote KnowledgeService.
 */
@Slf4j
@Component("auditRejectedListener")
public class AuditRejectedListener implements JavaDelegate {

    @DubboReference(check = false, timeout = 10000)
    private KnowledgeService knowledgeService;

    @Override
    public void execute(DelegateExecution execution) {
        Long knowledgeId = (Long) execution.getVariable("knowledgeId");
        Long version = (Long) execution.getVariable("version");
        String comment = (String) execution.getVariable("comment");
        
        log.info("Workflow REJECTED - Knowledge ID: {}, Version: {}, Comment: {}", knowledgeId, version, comment);
        
        if (knowledgeId == null) {
            log.warn("knowledgeId is null, skipping update");
            return;
        }
        
        try {
            if (version != null) {
                knowledgeService.rejectVersion(knowledgeId, version);
                log.info("Rejected version {} for knowledge {}", version, knowledgeId);
            } else {
                knowledgeService.updateKnowledgeStatus(knowledgeId, Constants.FILE_STATUS_REJECTED, false);
                log.info("Rejected knowledge {}", knowledgeId);
            }
        } catch (Exception e) {
            log.error("Failed to reject knowledge via workflow listener", e);
        }
    }
}
