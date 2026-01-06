package com.knowledge.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_relation")
public class KnowledgeRelation extends BaseEntity {
    private Long knowledgeId;
    private Long relatedKnowledgeId;
    private String relationType; // RELATED, REFERENCE, SIMILAR
}

