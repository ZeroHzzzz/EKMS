package com.knowledge.api.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 合并预览DTO（用于GitHub风格的合并界面）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergePreviewDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 知识ID
     */
    private Long knowledgeId;
    
    /**
     * 基础版本号（共同祖先）
     */
    private Long baseVersion;
    
    /**
     * 当前发布版本号（Ours/Target）
     */
    private Long currentVersion;
    
    /**
     * 当前发布版本的作者
     */
    private String currentVersionAuthor;
    
    /**
     * 草稿版本号（Theirs/Incoming）
     */
    private Long draftVersion;
    
    /**
     * 草稿作者
     */
    private String draftAuthor;
    
    /**
     * 是否有冲突
     */
    private boolean hasConflict;
    
    /**
     * 冲突/非冲突块数量
     */
    private int conflictBlockCount;
    
    /**
     * 合并块列表
     */
    private List<MergeBlock> blocks;
    
    /**
     * 冲突信息消息
     */
    private String message;
    
    /**
     * 合并块
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MergeBlock implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /**
         * 块ID（用于前端标识）
         */
        private int blockId;
        
        /**
         * 块类型：EQUAL（相同）, CURRENT_ONLY（仅当前版本有）, DRAFT_ONLY（仅草稿有）, CONFLICT（冲突）
         */
        private String type;
        
        /**
         * 基础版本内容
         */
        private String baseContent;
        
        /**
         * 当前版本内容
         */
        private String currentContent;
        
        /**
         * 草稿版本内容
         */
        private String draftContent;
        
        /**
         * 自动合并建议（如果可以自动合并）
         */
        private String autoMergedContent;
        
        /**
         * 行号范围开始（用于定位）
         */
        private int startLine;
        
        /**
         * 行号范围结束
         */
        private int endLine;
    }
}
