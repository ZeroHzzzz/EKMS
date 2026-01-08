package com.knowledge.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_version")
public class KnowledgeVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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
    private String commitMessage;     // Commit消息（变更说明的别名，为了兼容保留changeDescription）
    
    // 发布状态字段
    private Boolean isPublished;      // 是否为已发布版本
    private String status;            // 版本状态：DRAFT(草稿)、PENDING(待审核)、APPROVED(已发布)、REJECTED(已驳回)
    private Long mergeFromVersion;    // 来源版本（如果是合并产生的版本，记录来源草稿版本），用于图形化历史
    
    // 冲突检测字段
    private Long baseVersion;         // 基于哪个已发布版本创建（用于检测并发冲突）

    // Manual Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

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

    public String getChangeDescription() { return changeDescription; }
    public void setChangeDescription(String changeDescription) { this.changeDescription = changeDescription; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public Long getParentCommitId() { return parentCommitId; }
    public void setParentCommitId(Long parentCommitId) { this.parentCommitId = parentCommitId; }

    public String getCommitMessage() { return commitMessage; }
    public void setCommitMessage(String commitMessage) { this.commitMessage = commitMessage; }

    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getMergeFromVersion() { return mergeFromVersion; }
    public void setMergeFromVersion(Long mergeFromVersion) { this.mergeFromVersion = mergeFromVersion; }

    public Long getBaseVersion() { return baseVersion; }
    public void setBaseVersion(Long baseVersion) { this.baseVersion = baseVersion; }
}

