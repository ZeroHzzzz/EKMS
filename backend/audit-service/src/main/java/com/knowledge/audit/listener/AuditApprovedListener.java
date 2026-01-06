package com.knowledge.audit.listener;

import com.knowledge.api.service.KnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Flowable Listener: Triggered when audit is APPROVED.
 * Updates Knowledge status via remote KnowledgeService.
 */
@Slf4j
@Component("auditApprovedListener")
public class AuditApprovedListener implements JavaDelegate {

    @DubboReference(check = false, timeout = 10000)
    private KnowledgeService knowledgeService;

    @Override
    public void execute(DelegateExecution execution) {
        Long knowledgeId = (Long) execution.getVariable("knowledgeId");
        Long version = (Long) execution.getVariable("version");
        
        log.info("Workflow APPROVED - Knowledge ID: {}, Version: {}", knowledgeId, version);
        
        if (knowledgeId == null) {
            log.warn("knowledgeId is null, skipping update");
            return;
        }
        
        try {
            if (version != null) {
                knowledgeService.publishVersion(knowledgeId, version);
                log.info("Published version {} for knowledge {}", version, knowledgeId);
            } else {
                knowledgeService.publishKnowledge(knowledgeId);
                log.info("Published knowledge {}", knowledgeId);
            }
        } catch (Exception e) {
            log.error("Failed to publish knowledge via workflow listener", e);
        }
    }
}
