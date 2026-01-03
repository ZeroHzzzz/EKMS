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
    private String status; // 状态筛选
    private String author; // 作者筛选
    private String startDate; // 开始日期
    private String endDate; // 结束日期
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String sortField = "clickCount";
}

