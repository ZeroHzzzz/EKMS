package com.knowledge.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_knowledge_collection")
public class UserKnowledgeCollection extends BaseEntity {
    private Long userId;
    private Long knowledgeId;
}

