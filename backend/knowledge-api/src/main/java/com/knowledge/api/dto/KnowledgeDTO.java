package com.knowledge.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
    @JsonDeserialize(using = ParentIdDeserializer.class)
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
    private Boolean isDepartmentRoot; // 是否为部门根节点
    private String currentBranch; // 当前分支（用于版本控制）
    
    /**
     * Parsing full text content for search
     */
    private String contentText;
    
    /**
     * 自定义 setter 方法，处理 parentId 的容错
     * 如果传入的是负数，转换为 null（部门根节点不存在于数据库）
     * 注意：字符串格式的部门ID（如 "dept-1"）会由 ParentIdDeserializer 处理
     */
    public void setParentId(Long parentId) {
        if (parentId == null) {
            this.parentId = null;
            return;
        }
        
        // 如果是负数，说明是部门根节点，返回 null
        if (parentId < 0) {
            this.parentId = null;
        } else {
            this.parentId = parentId;
        }
    }
}

