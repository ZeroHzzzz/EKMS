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
    private Long version;  // 审核的版本号
    private String status;
    private Long submitUserId;
    private Long auditorId;
    private String comment;
    private List<String> validationErrors;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
    
    // 额外信息（用于列表展示）
    private String knowledgeTitle;  // 知识标题
    private String submitUserName;  // 提交人姓名
    private Long fileId;  // 文件ID（用于预览）
}

