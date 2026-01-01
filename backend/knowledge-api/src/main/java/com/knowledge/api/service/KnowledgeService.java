package com.knowledge.api.service;

import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.KnowledgeQueryDTO;
import com.knowledge.api.dto.KnowledgeVersionDTO;
import com.knowledge.api.dto.StatisticsDTO;

import java.util.List;

public interface KnowledgeService {
    KnowledgeDTO createKnowledge(KnowledgeDTO knowledgeDTO);
    KnowledgeDTO updateKnowledge(KnowledgeDTO knowledgeDTO);
    KnowledgeDTO getKnowledgeById(Long id);
    List<KnowledgeDTO> listKnowledge(KnowledgeQueryDTO queryDTO);
    boolean deleteKnowledge(Long id);
    void updateClickCount(Long id);
    void updateCollectCount(Long id, boolean collect);
    boolean publishKnowledge(Long id);
    List<KnowledgeDTO> getHotKnowledge(int limit);
    List<KnowledgeDTO> getRelatedKnowledge(Long id, int limit);
    StatisticsDTO getStatistics();
    
    // 收藏相关方法
    boolean collectKnowledge(Long userId, Long knowledgeId);
    boolean cancelCollectKnowledge(Long userId, Long knowledgeId);
    boolean isCollected(Long userId, Long knowledgeId);
    List<KnowledgeDTO> getMyCollections(Long userId);
    
    // 版本相关方法
    List<KnowledgeVersionDTO> getKnowledgeVersions(Long knowledgeId);
    KnowledgeVersionDTO getKnowledgeVersion(Long knowledgeId, Long version);
    KnowledgeVersionDTO.DiffResult compareVersions(Long knowledgeId, Long version1, Long version2);
}

