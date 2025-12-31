package com.knowledge.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit")
public class Audit extends BaseEntity {
    private Long knowledgeId;
    private String status;
    private Long submitUserId;
    private Long auditorId;
    private String comment;
    private String validationErrors;
}

