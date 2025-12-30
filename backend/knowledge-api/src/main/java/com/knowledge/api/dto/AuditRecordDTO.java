package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AuditRecordDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long auditId;
    private Long auditorId;
    private String auditorName;
    private String action; // SUBMIT, APPROVE, REJECT
    private String comment;
    private LocalDateTime auditTime;
}

