package com.knowledge.gateway.controller;

import com.knowledge.api.dto.*;
import com.knowledge.api.service.*;
import com.knowledge.common.result.Result;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return Result.success(result);
    }

    @PutMapping("/{id}")
    public Result<KnowledgeDTO> updateKnowledge(@PathVariable Long id, @RequestBody KnowledgeDTO knowledgeDTO) {
        knowledgeDTO.setId(id);
        KnowledgeDTO result = knowledgeService.updateKnowledge(knowledgeDTO);
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
        return Result.success(result);
    }

    @PostMapping("/search")
    public Result<SearchResultDTO> search(@RequestBody SearchRequestDTO request) {
        SearchResultDTO result = searchService.search(request);
        return Result.success(result);
    }

    @GetMapping("/hot")
    public Result<List<KnowledgeDTO>> getHotKnowledge(@RequestParam(defaultValue = "10") int limit) {
        List<KnowledgeDTO> result = knowledgeService.getHotKnowledge(limit);
        return Result.success(result);
    }

    @GetMapping("/{id}/related")
    public Result<List<KnowledgeDTO>> getRelatedKnowledge(@PathVariable Long id, @RequestParam(defaultValue = "5") int limit) {
        List<KnowledgeDTO> result = knowledgeService.getRelatedKnowledge(id, limit);
        return Result.success(result);
    }

    @PostMapping("/{id}/audit/submit")
    public Result<AuditDTO> submitForAudit(@PathVariable Long id, @RequestParam Long userId) {
        AuditDTO result = auditService.submitForAudit(id, userId);
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
}

