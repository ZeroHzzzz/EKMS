package com.knowledge.api.service;

import com.knowledge.api.dto.SearchResultDTO;
import com.knowledge.api.dto.SearchRequestDTO;

public interface SearchService {
    SearchResultDTO search(SearchRequestDTO request);
    void indexKnowledge(Long knowledgeId);
    void deleteIndex(Long knowledgeId);
    void updateIndex(Long knowledgeId);
}

