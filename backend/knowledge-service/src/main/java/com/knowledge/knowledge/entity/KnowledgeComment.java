package com.knowledge.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_comment")
public class KnowledgeComment extends BaseEntity {
    private Long knowledgeId;
    private Long userId;
    private Long parentId; // 父评论ID，支持回复
    private String content;
    private String status; // APPROVED, PENDING, DELETED
    private Integer likeCount;
}

