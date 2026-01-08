package com.knowledge.api.dto;

import java.io.Serializable;
import lombok.Data;

@Data
public class MergeStatusDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 是否可以自动无冲突合并
     */
    private boolean canAutoMerge;
    
    /**
     * 是否存在冲突
     */
    private boolean hasConflict;
    
    /**
     * 合并后的内容（如果无冲突，是最终内容；如果有冲突，包含冲突标记）
     */
    private String mergedContent;
    
    /**
     * 冲突详情描述（可选）
     */
    private String conflictDetails;
    
    /**
     * 基础版本号（共同祖先）
     */
    private Long baseVersion;
    
    /**
     * 目标版本号（当前最新发布版本）
     */
    private Long targetVersion;
    
    /**
     * 待合并版本号（草稿版本）
     */
    private Long incomingVersion;
}
