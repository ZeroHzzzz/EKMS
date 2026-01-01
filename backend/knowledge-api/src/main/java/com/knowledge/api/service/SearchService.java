package com.knowledge.api.service;

import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.SearchResultDTO;
import com.knowledge.api.dto.SearchRequestDTO;
import com.knowledge.api.dto.SearchSuggestionDTO;

public interface SearchService {
    SearchResultDTO search(SearchRequestDTO request);
    SearchSuggestionDTO getSuggestions(String keyword, int limit);
    void indexKnowledge(Long knowledgeId);
    void indexKnowledge(KnowledgeDTO knowledgeDTO);
    void deleteIndex(Long knowledgeId);
    void updateIndex(Long knowledgeId);
    void updateIndex(KnowledgeDTO knowledgeDTO);
}

