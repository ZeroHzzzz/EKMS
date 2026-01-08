package com.knowledge.api.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 合并解决请求DTO
 */
@Data
public class MergeResolveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 知识ID
     */
    private Long knowledgeId;
    
    /**
     * 基础版本号
     */
    private Long baseVersion;
    
    /**
     * 当前版本号
     */
    private Long currentVersion;
    
    /**
     * 草稿版本号
     */
    private Long draftVersion;
    
    /**
     * 每个冲突块的解决方案
     */
    private List<BlockResolution> resolutions;
    
    /**
     * 合并后的完整内容（如果前端直接编辑完整内容）
     */
    private String mergedContent;
    
    /**
     * 提交消息
     */
    private String commitMessage;
    
    /**
     * 是否强制覆盖（忽略冲突）
     */
    private boolean forceOverwrite;
    
    /**
     * 块解决方案
     */
    @Data
    public static class BlockResolution implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /**
         * 块ID
         */
        private int blockId;
        
        /**
         * 选择：CURRENT（使用当前版本）, DRAFT（使用草稿）, BOTH（两者都保留）, CUSTOM（自定义）
         */
        private String choice;
        
        /**
         * 自定义内容（如果choice是CUSTOM）
         */
        private String customContent;
    }
}
