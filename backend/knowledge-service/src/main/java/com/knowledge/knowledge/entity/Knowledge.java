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
    private Long version;
    private Long parentId;
    private Integer sortOrder;
}

