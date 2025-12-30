package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class KnowledgeQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;
    private String category;
    private String department;
    private String author;
    private String status;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String sortField = "clickCount";
    private String sortOrder = "DESC";
}

