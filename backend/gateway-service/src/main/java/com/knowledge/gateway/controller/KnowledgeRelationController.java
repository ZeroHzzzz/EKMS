package com.knowledge.gateway.controller;

import com.knowledge.api.dto.KnowledgeRelationDTO;
import com.knowledge.api.service.KnowledgeRelationService;
import com.knowledge.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/knowledge/relation")
public class KnowledgeRelationController {

    @DubboReference(check = false, timeout = 10000)
    private KnowledgeRelationService relationService;

    @PostMapping
    public Result<Boolean> addRelation(@RequestBody AddRelationRequest request) {
        try {
            boolean result = relationService.addRelation(
                request.getKnowledgeId(),
                request.getRelatedKnowledgeId(),
                request.getRelationType()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("添加关联失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{relationId}")
    public Result<Boolean> deleteRelation(@PathVariable Long relationId) {
        try {
            boolean result = relationService.deleteRelation(relationId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("删除关联失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping
    public Result<Boolean> deleteRelation(
            @RequestParam Long knowledgeId,
            @RequestParam Long relatedKnowledgeId) {
        try {
            boolean result = relationService.deleteRelation(knowledgeId, relatedKnowledgeId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("删除关联失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{knowledgeId}")
    public Result<List<KnowledgeRelationDTO>> getRelations(@PathVariable Long knowledgeId) {
        try {
            List<KnowledgeRelationDTO> result = relationService.getRelations(knowledgeId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取关联列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    // 内部类：添加关联请求
    @lombok.Data
    public static class AddRelationRequest {
        private Long knowledgeId;
        private Long relatedKnowledgeId;
        private String relationType;
    }
}

