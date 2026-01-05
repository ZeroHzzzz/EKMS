package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AuditRecordDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long auditId;
    private Long knowledgeId;
    private Long version;  // 审核的版本号
    private Long auditorId;
    private String auditorName;
    private String action; // SUBMIT, APPROVE, REJECT
    private String status; // PENDING, APPROVED, REJECTED
    private String comment;
    private LocalDateTime auditTime;
    private LocalDateTime submitTime; // 提交时间
    private String submitUserName; // 提交人姓名
}

