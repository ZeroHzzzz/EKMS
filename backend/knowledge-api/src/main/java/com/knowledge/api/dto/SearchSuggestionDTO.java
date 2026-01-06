package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class SearchSuggestionDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<String> suggestions; // 搜索建议列表
    private List<KnowledgeDTO> previewResults; // 预览结果（前几个匹配的知识）
}

