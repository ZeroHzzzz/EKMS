package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class BatchUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<Long> knowledgeIds;
    private Map<String, Object> updateData; // 要更新的字段和值
}

