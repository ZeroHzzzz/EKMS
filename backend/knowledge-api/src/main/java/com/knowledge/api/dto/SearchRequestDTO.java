package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class SearchRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;
    private String searchType; // FULL_TEXT, PINYIN, INITIAL
    private String category;
    private String fileType;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String sortField = "clickCount";
}

