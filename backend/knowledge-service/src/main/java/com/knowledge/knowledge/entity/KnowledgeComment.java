package com.knowledge.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_comment")
public class KnowledgeComment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeId;
    private Long userId;
    private Long parentId; // 父评论ID，支持回复
    private String content;
    private String status; // APPROVED, PENDING, DELETED
    private Integer likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
}

