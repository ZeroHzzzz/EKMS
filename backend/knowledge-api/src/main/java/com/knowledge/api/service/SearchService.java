package com.knowledge.api.service;

import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.SearchResultDTO;
import com.knowledge.api.dto.SearchRequestDTO;

public interface SearchService {
    SearchResultDTO search(SearchRequestDTO request);
    void indexKnowledge(Long knowledgeId);
    void indexKnowledge(KnowledgeDTO knowledgeDTO);
    void deleteIndex(Long knowledgeId);
    void updateIndex(Long knowledgeId);
    void updateIndex(KnowledgeDTO knowledgeDTO);
}

