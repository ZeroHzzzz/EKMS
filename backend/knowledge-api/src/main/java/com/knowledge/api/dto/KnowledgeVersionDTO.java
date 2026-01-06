package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class KnowledgeVersionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long knowledgeId;
    private Long version;
    private String title;
    private String content;
    private String summary;
    private String category;
    private String keywords;
    private String author;
    private String department;
    private Long fileId;
    private String changeDescription;
    private String createdBy;
    private LocalDateTime createTime;
    
    // Git风格版本管理字段
    private String commitHash;        // Commit哈希
    private String branch;            // 分支名称
    private Long parentCommitId;      // 父Commit ID
    private String commitMessage;     // Commit消息
    
    // 发布状态字段
    private Boolean isPublished;      // 是否为已发布版本
    private String status;            // 版本状态：DRAFT(草稿)、PENDING(待审核)、APPROVED(已发布)、REJECTED(已驳回)
    
    /**
     * 差异结果
     */
    @Data
    public static class DiffResult implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Long knowledgeId;
        private Long version1;
        private Long version2;
        private List<DiffLine> diffLines;
        private DiffStats stats;
        
        @Data
        public static class DiffLine implements Serializable {
            private static final long serialVersionUID = 1L;
            private String type; // EQUAL, INSERT, DELETE
            private String content;
            private int lineNumber1;
            private int lineNumber2;
        }
        
        @Data
        public static class DiffStats implements Serializable {
            private static final long serialVersionUID = 1L;
            private int insertCount;
            private int deleteCount;
            private int equalCount;
        }
    }
}

