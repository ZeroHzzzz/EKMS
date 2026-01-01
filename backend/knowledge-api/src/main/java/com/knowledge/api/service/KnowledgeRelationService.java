package com.knowledge.api.service;

import com.knowledge.api.dto.KnowledgeRelationDTO;
import java.util.List;

public interface KnowledgeRelationService {
    boolean addRelation(Long knowledgeId, Long relatedKnowledgeId, String relationType);
    boolean deleteRelation(Long relationId);
    boolean deleteRelation(Long knowledgeId, Long relatedKnowledgeId);
    List<KnowledgeRelationDTO> getRelations(Long knowledgeId);
}

