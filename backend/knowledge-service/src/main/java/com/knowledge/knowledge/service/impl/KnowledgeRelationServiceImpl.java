package com.knowledge.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.KnowledgeRelationDTO;
import com.knowledge.api.service.KnowledgeRelationService;
import com.knowledge.api.service.KnowledgeService;
import com.knowledge.knowledge.entity.KnowledgeRelation;
import com.knowledge.knowledge.mapper.KnowledgeRelationMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@DubboService
public class KnowledgeRelationServiceImpl implements KnowledgeRelationService {

    @Resource
    private KnowledgeRelationMapper relationMapper;

    @DubboReference(check = false, timeout = 10000)
    private KnowledgeService knowledgeService;

    @Override
    @Transactional

    public boolean addRelation(Long knowledgeId, Long relatedKnowledgeId, String relationType) {
        // 不能关联自己
        if (knowledgeId.equals(relatedKnowledgeId)) {
            throw new RuntimeException("不能关联自己");
        }

        // 1. 检查正向关联是否存在
        LambdaQueryWrapper<KnowledgeRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeRelation::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgeRelation::getRelatedKnowledgeId, relatedKnowledgeId);
        
        boolean exists = relationMapper.selectCount(wrapper) > 0;
        
        if (!exists) {
            // 添加正向关联
            KnowledgeRelation relation = new KnowledgeRelation();
            relation.setKnowledgeId(knowledgeId);
            relation.setRelatedKnowledgeId(relatedKnowledgeId);
            relation.setRelationType(relationType != null ? relationType : "RELATED");
            relation.setCreateTime(LocalDateTime.now());
            relationMapper.insert(relation);
        }
        
        // 2. 检查反向关联是否存在 (Bidirectional)
        LambdaQueryWrapper<KnowledgeRelation> reverseWrapper = new LambdaQueryWrapper<>();
        reverseWrapper.eq(KnowledgeRelation::getKnowledgeId, relatedKnowledgeId);
        reverseWrapper.eq(KnowledgeRelation::getRelatedKnowledgeId, knowledgeId);
        
        boolean reverseExists = relationMapper.selectCount(reverseWrapper) > 0;
        
        if (!reverseExists) {
            // 添加反向关联
            KnowledgeRelation reverseRelation = new KnowledgeRelation();
            reverseRelation.setKnowledgeId(relatedKnowledgeId);
            reverseRelation.setRelatedKnowledgeId(knowledgeId);
            reverseRelation.setRelationType(relationType != null ? relationType : "RELATED");
            reverseRelation.setCreateTime(LocalDateTime.now());
            relationMapper.insert(reverseRelation);
        }
        
        return !exists || !reverseExists;
    }

    @Override
    @Transactional
    public boolean deleteRelation(Long relationId) {
        // 先查询该关联，以便找到反向关联
        KnowledgeRelation relation = relationMapper.selectById(relationId);
        if (relation == null) {
            return false;
        }
        
        Long kId = relation.getKnowledgeId();
        Long rId = relation.getRelatedKnowledgeId();
        
        // 删除正向
        int rows = relationMapper.deleteById(relationId);
        
        // 删除反向
        LambdaQueryWrapper<KnowledgeRelation> reverseWrapper = new LambdaQueryWrapper<>();
        reverseWrapper.eq(KnowledgeRelation::getKnowledgeId, rId);
        reverseWrapper.eq(KnowledgeRelation::getRelatedKnowledgeId, kId);
        relationMapper.delete(reverseWrapper);
        
        return rows > 0;
    }

    @Override
    @Transactional
    public boolean deleteRelation(Long knowledgeId, Long relatedKnowledgeId) {
        // 删除正向
        LambdaQueryWrapper<KnowledgeRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeRelation::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgeRelation::getRelatedKnowledgeId, relatedKnowledgeId);
        int rows = relationMapper.delete(wrapper);
        
        // 删除反向
        LambdaQueryWrapper<KnowledgeRelation> reverseWrapper = new LambdaQueryWrapper<>();
        reverseWrapper.eq(KnowledgeRelation::getKnowledgeId, relatedKnowledgeId);
        reverseWrapper.eq(KnowledgeRelation::getRelatedKnowledgeId, knowledgeId);
        relationMapper.delete(reverseWrapper);
        
        return rows > 0;
    }

    @Override
    public List<KnowledgeRelationDTO> getRelations(Long knowledgeId) {
        LambdaQueryWrapper<KnowledgeRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeRelation::getKnowledgeId, knowledgeId);
        wrapper.orderByDesc(KnowledgeRelation::getCreateTime);
        
        List<KnowledgeRelation> relations = relationMapper.selectList(wrapper);
        
        return relations.stream().map(relation -> {
            KnowledgeRelationDTO dto = new KnowledgeRelationDTO();
            BeanUtils.copyProperties(relation, dto);
            
            // 加载关联的知识详情
            try {
                KnowledgeDTO relatedKnowledge = knowledgeService.getKnowledgeById(relation.getRelatedKnowledgeId());
                dto.setRelatedKnowledge(relatedKnowledge);
            } catch (Exception e) {
                log.warn("加载关联知识失败: relatedKnowledgeId={}", relation.getRelatedKnowledgeId(), e);
            }
            
            return dto;
        }).collect(Collectors.toList());
    }
    @Override
    @Transactional
    public void deleteByKnowledgeId(Long knowledgeId) {
        // 删除该知识作为源或目标的关联关系
        LambdaQueryWrapper<KnowledgeRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeRelation::getKnowledgeId, knowledgeId)
               .or()
               .eq(KnowledgeRelation::getRelatedKnowledgeId, knowledgeId);
        
        int deleted = relationMapper.delete(wrapper);
        log.info("已删除知识 ID={} 关联的 {} 条关系数据", knowledgeId, deleted);
    }
}

