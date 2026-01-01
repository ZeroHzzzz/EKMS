package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class KnowledgeRelationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long knowledgeId;
    private Long relatedKnowledgeId;
    private String relationType; // RELATED, REFERENCE, SIMILAR
    private KnowledgeDTO relatedKnowledge; // 关联的知识详情
}

