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
}

