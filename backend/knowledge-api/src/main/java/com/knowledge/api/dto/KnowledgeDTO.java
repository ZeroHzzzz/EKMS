package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class KnowledgeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    private String summary;
    private String category;
    private String keywords;
    private String author;
    private String department;
    private Long fileId;
    private String status;
    private Long clickCount;
    private Long collectCount;
    private Long version;
    private Long parentId;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
    private String changeDescription; // 变更说明
    private List<Long> relatedKnowledgeIds;
    private List<String> tags;
    private HighlightDTO highlight; // 搜索高亮信息（仅搜索时使用）
}

