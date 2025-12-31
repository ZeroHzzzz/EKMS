package com.knowledge.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.KnowledgeQueryDTO;
import com.knowledge.api.service.KnowledgeService;
import com.knowledge.common.constant.Constants;
import com.knowledge.knowledge.entity.Knowledge;
import com.knowledge.knowledge.mapper.KnowledgeMapper;
import lombok.extern.slf4j.Slf4j;
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
public class KnowledgeServiceImpl implements KnowledgeService {

    @Resource
    private KnowledgeMapper knowledgeMapper;

    @Override
    @Transactional
    public KnowledgeDTO createKnowledge(KnowledgeDTO knowledgeDTO) {
        Knowledge knowledge = new Knowledge();
        BeanUtils.copyProperties(knowledgeDTO, knowledge);
        knowledge.setStatus(Constants.FILE_STATUS_DRAFT);
        knowledge.setClickCount(0L);
        knowledge.setCollectCount(0L);
        knowledge.setVersion(1L);
        knowledge.setCreateTime(LocalDateTime.now());
        knowledgeMapper.insert(knowledge);
        
        KnowledgeDTO result = new KnowledgeDTO();
        BeanUtils.copyProperties(knowledge, result);
        return result;
    }

    @Override
    @Transactional
    public KnowledgeDTO updateKnowledge(KnowledgeDTO knowledgeDTO) {
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeDTO.getId());
        if (knowledge == null) {
            throw new RuntimeException("知识不存在");
        }
        
        BeanUtils.copyProperties(knowledgeDTO, knowledge, "id", "createTime", "createBy");
        knowledge.setVersion(knowledge.getVersion() + 1);
        knowledge.setUpdateTime(LocalDateTime.now());
        knowledgeMapper.updateById(knowledge);
        
        KnowledgeDTO result = new KnowledgeDTO();
        BeanUtils.copyProperties(knowledge, result);
        return result;
    }

    @Override
    public KnowledgeDTO getKnowledgeById(Long id) {
        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            return null;
        }
        KnowledgeDTO knowledgeDTO = new KnowledgeDTO();
        BeanUtils.copyProperties(knowledge, knowledgeDTO);
        return knowledgeDTO;
    }

    @Override
    public List<KnowledgeDTO> listKnowledge(KnowledgeQueryDTO queryDTO) {
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        
        if (queryDTO.getCategory() != null) {
            wrapper.eq(Knowledge::getCategory, queryDTO.getCategory());
        }
        if (queryDTO.getDepartment() != null) {
            wrapper.eq(Knowledge::getDepartment, queryDTO.getDepartment());
        }
        if (queryDTO.getAuthor() != null) {
            wrapper.eq(Knowledge::getAuthor, queryDTO.getAuthor());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Knowledge::getStatus, queryDTO.getStatus());
        }
        
        // 排序
        if ("clickCount".equals(queryDTO.getSortField())) {
            if ("DESC".equals(queryDTO.getSortOrder())) {
                wrapper.orderByDesc(Knowledge::getClickCount);
            } else {
                wrapper.orderByAsc(Knowledge::getClickCount);
            }
        }
        
        Page<Knowledge> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<Knowledge> result = knowledgeMapper.selectPage(page, wrapper);
        
        return result.getRecords().stream().map(knowledge -> {
            KnowledgeDTO dto = new KnowledgeDTO();
            BeanUtils.copyProperties(knowledge, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean deleteKnowledge(Long id) {
        return knowledgeMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public void updateClickCount(Long id) {
        knowledgeMapper.incrementClickCount(id);
    }

    @Override
    @Transactional
    public void updateCollectCount(Long id, boolean collect) {
        knowledgeMapper.updateCollectCount(id, collect ? 1 : -1);
    }

    @Override
    public List<KnowledgeDTO> getHotKnowledge(int limit) {
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Knowledge::getStatus, Constants.FILE_STATUS_APPROVED);
        wrapper.orderByDesc(Knowledge::getClickCount);
        wrapper.last("LIMIT " + limit);
        
        List<Knowledge> knowledges = knowledgeMapper.selectList(wrapper);
        return knowledges.stream().map(knowledge -> {
            KnowledgeDTO dto = new KnowledgeDTO();
            BeanUtils.copyProperties(knowledge, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeDTO> getRelatedKnowledge(Long id, int limit) {
        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            return List.of();
        }
        
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Knowledge::getCategory, knowledge.getCategory());
        wrapper.ne(Knowledge::getId, id);
        wrapper.eq(Knowledge::getStatus, Constants.FILE_STATUS_APPROVED);
        wrapper.orderByDesc(Knowledge::getClickCount);
        wrapper.last("LIMIT " + limit);
        
        List<Knowledge> knowledges = knowledgeMapper.selectList(wrapper);
        return knowledges.stream().map(k -> {
            KnowledgeDTO dto = new KnowledgeDTO();
            BeanUtils.copyProperties(k, dto);
            return dto;
        }).collect(Collectors.toList());
    }
}

