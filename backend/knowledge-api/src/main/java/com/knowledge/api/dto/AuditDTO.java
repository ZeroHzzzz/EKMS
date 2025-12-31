package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AuditDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long knowledgeId;
    private String status;
    private Long submitUserId;
    private Long auditorId;
    private String comment;
    private List<String> validationErrors;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
}

