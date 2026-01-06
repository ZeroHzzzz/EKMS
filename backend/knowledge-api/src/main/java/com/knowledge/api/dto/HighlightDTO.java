package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 搜索高亮信息
 */
@Data
public class HighlightDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<String> title;      // 标题中的高亮片段
    private List<String> content;    // 内容中的高亮片段
    private List<String> keywords;   // 关键词中的高亮片段
    private List<String> fileName;   // 文件名中的高亮片段
    
    /**
     * 获取匹配位置描述
     */
    public String getMatchLocation() {
        StringBuilder location = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            location.append("标题");
        }
        if (fileName != null && !fileName.isEmpty()) {
            if (location.length() > 0) location.append("、");
            location.append("文件名");
        }
        if (keywords != null && !keywords.isEmpty()) {
            if (location.length() > 0) location.append("、");
            location.append("关键词");
        }
        if (content != null && !content.isEmpty()) {
            if (location.length() > 0) location.append("、");
            location.append("内容");
        }
        return location.length() > 0 ? "匹配位置：" + location.toString() : "";
    }
}

