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
}

