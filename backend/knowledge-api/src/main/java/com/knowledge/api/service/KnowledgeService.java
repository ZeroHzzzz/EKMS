package com.knowledge.api.service;

import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.KnowledgeQueryDTO;

import java.util.List;

public interface KnowledgeService {
    KnowledgeDTO createKnowledge(KnowledgeDTO knowledgeDTO);
    KnowledgeDTO updateKnowledge(KnowledgeDTO knowledgeDTO);
    KnowledgeDTO getKnowledgeById(Long id);
    List<KnowledgeDTO> listKnowledge(KnowledgeQueryDTO queryDTO);
    boolean deleteKnowledge(Long id);
    void updateClickCount(Long id);
    void updateCollectCount(Long id, boolean collect);
    List<KnowledgeDTO> getHotKnowledge(int limit);
    List<KnowledgeDTO> getRelatedKnowledge(Long id, int limit);
}

