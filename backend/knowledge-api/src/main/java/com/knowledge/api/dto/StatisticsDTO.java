package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;

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
}

