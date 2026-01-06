package com.knowledge.api.service;

import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.SearchResultDTO;
import com.knowledge.api.dto.SearchRequestDTO;
import com.knowledge.api.dto.SearchSuggestionDTO;

import java.util.List;

public interface SearchService {
    SearchResultDTO search(SearchRequestDTO request);
    SearchSuggestionDTO getSuggestions(String keyword, int limit);
    void indexKnowledge(Long knowledgeId);
    void indexKnowledge(KnowledgeDTO knowledgeDTO);
    void deleteIndex(Long knowledgeId);
    void updateIndex(Long knowledgeId);
    void updateIndex(KnowledgeDTO knowledgeDTO);
    
    /**
     * 删除整个索引（用于重建索引前清理）
     */
    void deleteAllIndex();
    
    /**
     * 重建所有知识索引
     * 包含文档全文内容提取和索引
     * @param knowledgeList 所有知识列表
     * @return 成功索引的数量
     */
    int rebuildAllIndex(List<KnowledgeDTO> knowledgeList);
    
    /**
     * 语义搜索 - 使用 more_like_this 查询查找相似文档
     * @param text 输入文本（自然语言查询）
     * @param limit 返回结果数量限制
     * @return 相似知识列表
     */
    List<KnowledgeDTO> semanticSearch(String text, int limit);
}

