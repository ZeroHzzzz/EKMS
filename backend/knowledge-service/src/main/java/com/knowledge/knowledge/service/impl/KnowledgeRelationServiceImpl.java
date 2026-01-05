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
        // 检查是否已存在
        LambdaQueryWrapper<KnowledgeRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeRelation::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgeRelation::getRelatedKnowledgeId, relatedKnowledgeId);
        
        if (relationMapper.selectCount(wrapper) > 0) {
            return false; // 已存在
        }
        
        // 不能关联自己
        if (knowledgeId.equals(relatedKnowledgeId)) {
            throw new RuntimeException("不能关联自己");
        }
        
        // 添加关联
        KnowledgeRelation relation = new KnowledgeRelation();
        relation.setKnowledgeId(knowledgeId);
        relation.setRelatedKnowledgeId(relatedKnowledgeId);
        relation.setRelationType(relationType != null ? relationType : "RELATED");
        relation.setCreateTime(LocalDateTime.now());
        
        relationMapper.insert(relation);
        
        // 同时创建反向关联（可选，根据需求决定）
        // KnowledgeRelation reverseRelation = new KnowledgeRelation();
        // reverseRelation.setKnowledgeId(relatedKnowledgeId);
        // reverseRelation.setRelatedKnowledgeId(knowledgeId);
        // reverseRelation.setRelationType(relationType);
        // relationMapper.insert(reverseRelation);
        
        return true;
    }

    @Override
    @Transactional
    public boolean deleteRelation(Long relationId) {
        return relationMapper.deleteById(relationId) > 0;
    }

    @Override
    @Transactional
    public boolean deleteRelation(Long knowledgeId, Long relatedKnowledgeId) {
        LambdaQueryWrapper<KnowledgeRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeRelation::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgeRelation::getRelatedKnowledgeId, relatedKnowledgeId);
        
        return relationMapper.delete(wrapper) > 0;
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

