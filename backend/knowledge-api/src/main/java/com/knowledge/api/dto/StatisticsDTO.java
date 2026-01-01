package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 统计数据DTO
 */
@Data
public class StatisticsDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long totalKnowledge;      // 知识总数
    private Long totalClicks;        // 总点击量
    private Long totalCollections;   // 总收藏量
    private Long pendingAudit;        // 待审核数量
    
    // 新增统计维度
    private Double averageClickRate;  // 平均点击率（点击量/知识数）
    private Double averageCollectRate; // 平均收藏率（收藏量/知识数）
    private Double collectClickRatio;  // 收藏点击比（收藏量/点击量）
    
    // 分类统计
    private List<CategoryStatDTO> categoryStats; // 各分类的知识数量
    
    // 时间趋势（最近7天）
    private List<TrendDataDTO> clickTrend;  // 点击量趋势
    private List<TrendDataDTO> collectTrend; // 收藏量趋势
    
    // 热门知识（Top 10）
    private List<KnowledgeDTO> hotKnowledge;
    
    // 内部类：分类统计
    @Data
    public static class CategoryStatDTO implements Serializable {
        private String category;
        private Long count;
        private Long clicks;
        private Long collections;
    }
    
    // 内部类：趋势数据
    @Data
    public static class TrendDataDTO implements Serializable {
        private String date;  // 日期（YYYY-MM-DD）
        private Long value;   // 数值
    }
}

