package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class SearchResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<KnowledgeDTO> results;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private Long tookTime; // 搜索耗时(ms)
}

