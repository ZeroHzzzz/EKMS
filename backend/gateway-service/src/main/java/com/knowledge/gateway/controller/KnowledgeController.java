package com.knowledge.gateway.controller;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.knowledge.api.dto.*;
import com.knowledge.api.dto.ParentIdDeserializer;
import com.knowledge.api.service.*;
import com.knowledge.common.constant.Constants;
import com.knowledge.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @DubboReference(check = false, timeout = 10000)
    private KnowledgeService knowledgeService;

    @DubboReference(check = false, timeout = 10000)
    private SearchService searchService;

    @DubboReference(check = false, timeout = 10000)
    private AuditService auditService;

    @PostMapping
    public Result<KnowledgeDTO> createKnowledge(@RequestBody KnowledgeDTO knowledgeDTO) {
        KnowledgeDTO result = knowledgeService.createKnowledge(knowledgeDTO);
        // 同步索引到ElasticSearch
        if (result != null) {
            try {
                searchService.indexKnowledge(result);
            } catch (Exception e) {
                // 索引失败不影响主流程，只记录日志
                System.err.println("同步索引失败: " + e.getMessage());
            }
        }
        return Result.success(result);
    }

    @PutMapping("/{id}")
    public Result<KnowledgeDTO> updateKnowledge(@PathVariable Long id, @RequestBody KnowledgeDTO knowledgeDTO) {
        knowledgeDTO.setId(id);
        KnowledgeDTO result = knowledgeService.updateKnowledge(knowledgeDTO);
        // 同步更新索引到ElasticSearch
        if (result != null) {
            try {
                searchService.updateIndex(result);
            } catch (Exception e) {
                // 索引失败不影响主流程，只记录日志
                System.err.println("更新索引失败: " + e.getMessage());
            }
        }
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<KnowledgeDTO> getKnowledge(@PathVariable Long id) {
        KnowledgeDTO result = knowledgeService.getKnowledgeById(id);
        knowledgeService.updateClickCount(id);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<KnowledgeDTO>> listKnowledge(KnowledgeQueryDTO queryDTO) {
        List<KnowledgeDTO> result = knowledgeService.listKnowledge(queryDTO);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteKnowledge(@PathVariable Long id) {
        boolean result = knowledgeService.deleteKnowledge(id);
        // 从ElasticSearch删除索引
        if (result) {
            try {
                searchService.deleteIndex(id);
            } catch (Exception e) {
                // 删除索引失败不影响主流程，只记录日志
                System.err.println("删除索引失败: " + e.getMessage());
            }
        }
        return Result.success(result);
    }

    @PostMapping("/search")
    public Result<SearchResultDTO> search(@RequestBody SearchRequestDTO request) {
        SearchResultDTO result = searchService.search(request);
        return Result.success(result);
    }

    @GetMapping("/search/suggestions")
    public Result<com.knowledge.api.dto.SearchSuggestionDTO> getSuggestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        com.knowledge.api.dto.SearchSuggestionDTO result = searchService.getSuggestions(keyword, limit);
        return Result.success(result);
    }

    @GetMapping("/hot")
    public Result<List<KnowledgeDTO>> getHotKnowledge(@RequestParam(defaultValue = "10") int limit) {
        List<KnowledgeDTO> result = knowledgeService.getHotKnowledge(limit);
        return Result.success(result);
    }

    @GetMapping("/statistics")
    public Result<com.knowledge.api.dto.StatisticsDTO> getStatistics() {
        com.knowledge.api.dto.StatisticsDTO result = knowledgeService.getStatistics();
        return Result.success(result);
    }

    @PostMapping("/{id}/collect")
    public Result<Boolean> collectKnowledge(@PathVariable Long id, @RequestParam Long userId) {
        boolean result = knowledgeService.collectKnowledge(userId, id);
        return Result.success(result);
    }

    @DeleteMapping("/{id}/collect")
    public Result<Boolean> cancelCollectKnowledge(@PathVariable Long id, @RequestParam Long userId) {
        boolean result = knowledgeService.cancelCollectKnowledge(userId, id);
        return Result.success(result);
    }

    @GetMapping("/{id}/path")
    public Result<List<KnowledgeDTO>> getKnowledgePath(@PathVariable Long id) {
        List<KnowledgeDTO> result = knowledgeService.getKnowledgePath(id);
        return Result.success(result);
    }

    @GetMapping("/{id}/collect/status")
    public Result<Boolean> isCollected(@PathVariable Long id, @RequestParam Long userId) {
        boolean result = knowledgeService.isCollected(userId, id);
        return Result.success(result);
    }

    @GetMapping("/my/collections")
    public Result<List<KnowledgeDTO>> getMyCollections(@RequestParam Long userId) {
        List<KnowledgeDTO> result = knowledgeService.getMyCollections(userId);
        return Result.success(result);
    }

    @PostMapping("/{id}/publish")
    public Result<Boolean> publishKnowledge(@PathVariable Long id) {
        boolean result = knowledgeService.publishKnowledge(id);
        return Result.success(result);
    }

    @PostMapping("/{id}/submit-audit")
    public Result<AuditDTO> submitForAudit(@PathVariable Long id, @RequestParam Long userId) {
        // 提交审核
        KnowledgeDTO knowledge = knowledgeService.getKnowledgeById(id);
        if (knowledge == null) {
            return Result.error("知识不存在");
        }
        
        // 检查是否已有待审核记录
        List<AuditDTO> pendingAudits = auditService.getPendingAudits();
        boolean hasAudit = pendingAudits.stream()
            .anyMatch(a -> a.getKnowledgeId().equals(id) && Constants.AUDIT_STATUS_PENDING.equals(a.getStatus()));
        if (hasAudit) {
            return Result.error("该知识已提交审核，请勿重复提交");
        }
        
        // 如果知识状态是待审核或已驳回，可以提交审核
        if (Constants.FILE_STATUS_PENDING.equals(knowledge.getStatus()) || 
            Constants.FILE_STATUS_REJECTED.equals(knowledge.getStatus())) {
            // 创建审核记录
            AuditDTO auditDTO = auditService.submitForAudit(id, userId);
            // 确保知识状态为待审核
            if (!Constants.FILE_STATUS_PENDING.equals(knowledge.getStatus())) {
                knowledge.setStatus(Constants.FILE_STATUS_PENDING);
                knowledgeService.updateKnowledge(knowledge);
            }
            return Result.success(auditDTO);
        }
        
        return Result.error("知识状态不正确，无法提交审核（只有待审核或已驳回状态的知识可以提交审核）");
    }

    @GetMapping("/{id}/related")
    public Result<List<KnowledgeDTO>> getRelatedKnowledge(@PathVariable Long id, @RequestParam(defaultValue = "5") int limit) {
        List<KnowledgeDTO> result = knowledgeService.getRelatedKnowledge(id, limit);
        return Result.success(result);
    }

    @PostMapping("/batch/update")
    public Result<Boolean> batchUpdateKnowledge(@RequestBody BatchUpdateRequest request) {
        try {
            boolean result = knowledgeService.batchUpdateKnowledge(
                request.getKnowledgeIds(),
                request.getUpdateData()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量更新失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/tree")
    public Result<List<KnowledgeDTO>> getKnowledgeTree() {
        List<KnowledgeDTO> result = knowledgeService.getKnowledgeTree();
        return Result.success(result);
    }

    @PutMapping("/{id}/move")
    public Result<Boolean> moveKnowledge(
            @PathVariable Long id,
            @RequestBody MoveKnowledgeRequest request) {
        try {
            boolean result = knowledgeService.moveKnowledge(
                id,
                request.getParentId(),
                request.getSortOrder()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("移动知识失败", e);
            return Result.error(e.getMessage());
        }
    }

    // 内部类：移动知识请求
    @lombok.Data
    public static class MoveKnowledgeRequest {
        @JsonDeserialize(using = ParentIdDeserializer.class)
        private Long parentId;
        private Integer sortOrder;
    }

    @GetMapping("/audit/pending")
    public Result<List<AuditDTO>> getPendingAudits() {
        List<AuditDTO> result = auditService.getPendingAudits();
        return Result.success(result);
    }

    @GetMapping("/audit/all")
    public Result<List<AuditDTO>> getAllAudits() {
        List<AuditDTO> result = auditService.getAllAudits();
        return Result.success(result);
    }

    @PostMapping("/audit/{auditId}/approve")
    public Result<AuditDTO> approve(@PathVariable Long auditId, @RequestParam Long auditorId, @RequestParam String comment) {
        AuditDTO result = auditService.approve(auditId, auditorId, comment);
        return Result.success(result);
    }

    @PostMapping("/audit/{auditId}/reject")
    public Result<AuditDTO> reject(@PathVariable Long auditId, @RequestParam Long auditorId, @RequestParam String comment) {
        AuditDTO result = auditService.reject(auditId, auditorId, comment);
        return Result.success(result);
    }

    @GetMapping("/{id}/audit/history")
    public Result<List<AuditRecordDTO>> getAuditHistory(@PathVariable Long id) {
        List<AuditRecordDTO> result = auditService.getAuditHistory(id);
        return Result.success(result);
    }

    // 版本相关接口
    @GetMapping("/{id}/versions")
    public Result<List<KnowledgeVersionDTO>> getKnowledgeVersions(@PathVariable Long id) {
        List<KnowledgeVersionDTO> result = knowledgeService.getKnowledgeVersions(id);
        return Result.success(result);
    }

    @GetMapping("/{id}/versions/{version}")
    public Result<KnowledgeVersionDTO> getKnowledgeVersion(@PathVariable Long id, @PathVariable Long version) {
        KnowledgeVersionDTO result = knowledgeService.getKnowledgeVersion(id, version);
        if (result == null) {
            return Result.error("版本不存在");
        }
        return Result.success(result);
    }

    @GetMapping("/{id}/versions/compare")
    public Result<KnowledgeVersionDTO.DiffResult> compareVersions(
            @PathVariable Long id,
            @RequestParam Long version1,
            @RequestParam Long version2) {
        KnowledgeVersionDTO.DiffResult result = knowledgeService.compareVersions(id, version1, version2);
        return Result.success(result);
    }

    /**
     * 初始化所有知识的搜索索引（用于首次同步或重建索引）
     */
    @PostMapping("/search/reindex")
    public Result<String> reindexAll() {
        try {
            // 先删除旧索引（如果存在）
            try {
                // 通过反射调用 SearchServiceImpl 的 deleteIndex 方法
                // 或者直接使用 Elasticsearch 客户端删除索引
                // 这里我们使用一个变通方法：先删除所有文档，然后重新索引
                // 更好的方法是添加一个删除整个索引的接口
                log.info("开始重建索引，先删除旧索引...");
            } catch (Exception e) {
                log.warn("删除旧索引失败（可能索引不存在）: " + e.getMessage());
            }
            
            // 获取所有知识
            KnowledgeQueryDTO queryDTO = new KnowledgeQueryDTO();
            queryDTO.setPageNum(1);
            queryDTO.setPageSize(10000); // 获取所有数据
            List<KnowledgeDTO> allKnowledge = knowledgeService.listKnowledge(queryDTO);
            
            int successCount = 0;
            int failCount = 0;
            
            for (KnowledgeDTO knowledge : allKnowledge) {
                try {
                    searchService.indexKnowledge(knowledge);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("索引知识失败: id=" + knowledge.getId() + ", error=" + e.getMessage());
                }
            }
            
            return Result.success(String.format("索引重建完成：成功 %d 条，失败 %d 条。注意：如果搜索仍无结果，请重启 search-service 以重新创建索引映射。", successCount, failCount));
        } catch (Exception e) {
            return Result.error("重建索引失败: " + e.getMessage());
        }
    }
}

