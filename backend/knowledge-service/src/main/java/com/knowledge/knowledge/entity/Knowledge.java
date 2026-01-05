package com.knowledge.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge")
public class Knowledge extends BaseEntity {
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
    private Long version;              // 最新版本号
    private Long publishedVersion;     // 已发布的版本号（用户查看的版本）
    private Boolean hasDraft;          // 是否有待审核的草稿版本
    private Long parentId;
    private Integer sortOrder;
    
    // Git风格版本管理字段
    private String currentBranch;      // 当前分支
    private String currentCommitHash;  // 当前Commit Hash
    
    /**
     * Parsing full text content for search
     */
    private String contentText;

    // Manual Getters and Setters to ensure compilation success
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getClickCount() { return clickCount; }
    public void setClickCount(Long clickCount) { this.clickCount = clickCount; }

    public Long getCollectCount() { return collectCount; }
    public void setCollectCount(Long collectCount) { this.collectCount = collectCount; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Long getPublishedVersion() { return publishedVersion; }
    public void setPublishedVersion(Long publishedVersion) { this.publishedVersion = publishedVersion; }

    public Boolean getHasDraft() { return hasDraft; }
    public void setHasDraft(Boolean hasDraft) { this.hasDraft = hasDraft; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getCurrentBranch() { return currentBranch; }
    public void setCurrentBranch(String currentBranch) { this.currentBranch = currentBranch; }

    public String getCurrentCommitHash() { return currentCommitHash; }
    public void setCurrentCommitHash(String currentCommitHash) { this.currentCommitHash = currentCommitHash; }

    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
}
