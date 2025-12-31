package com.knowledge.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.KnowledgeQueryDTO;
import com.knowledge.api.dto.KnowledgeVersionDTO;
import com.knowledge.api.service.KnowledgeService;
import com.knowledge.common.util.DiffUtil;
import com.knowledge.common.constant.Constants;
import com.knowledge.knowledge.entity.Knowledge;
import com.knowledge.knowledge.entity.KnowledgeVersion;
import com.knowledge.knowledge.mapper.KnowledgeMapper;
import com.knowledge.knowledge.mapper.KnowledgeVersionMapper;
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
    
    @Resource
    private KnowledgeVersionMapper knowledgeVersionMapper;

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
        // 设置创建人
        if (knowledgeDTO.getCreateBy() != null) {
            knowledge.setCreateBy(knowledgeDTO.getCreateBy());
        }
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
        
        // 保存当前版本到版本历史表
        saveVersionHistory(knowledge, knowledgeDTO.getChangeDescription(), knowledgeDTO.getUpdateBy());
        
        // 更新知识内容
        BeanUtils.copyProperties(knowledgeDTO, knowledge, "id", "createTime", "createBy");
        knowledge.setVersion(knowledge.getVersion() + 1);
        knowledge.setUpdateTime(LocalDateTime.now());
        knowledgeMapper.updateById(knowledge);
        
        KnowledgeDTO result = new KnowledgeDTO();
        BeanUtils.copyProperties(knowledge, result);
        return result;
    }
    
    /**
     * 保存版本历史
     */
    private void saveVersionHistory(Knowledge knowledge, String changeDescription, String updateBy) {
        KnowledgeVersion version = new KnowledgeVersion();
        version.setKnowledgeId(knowledge.getId());
        version.setVersion(knowledge.getVersion());
        version.setTitle(knowledge.getTitle());
        version.setContent(knowledge.getContent());
        version.setSummary(knowledge.getSummary());
        version.setCategory(knowledge.getCategory());
        version.setKeywords(knowledge.getKeywords());
        version.setAuthor(knowledge.getAuthor());
        version.setDepartment(knowledge.getDepartment());
        version.setFileId(knowledge.getFileId());
        version.setChangeDescription(changeDescription);
        version.setCreatedBy(updateBy != null ? updateBy : knowledge.getUpdateBy());
        version.setCreateTime(LocalDateTime.now());
        
        knowledgeVersionMapper.insert(version);
        log.info("保存版本历史 - 知识ID: {}, 版本: {}", knowledge.getId(), knowledge.getVersion());
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
    
    @Override
    public List<KnowledgeVersionDTO> getKnowledgeVersions(Long knowledgeId) {
        LambdaQueryWrapper<KnowledgeVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        wrapper.orderByDesc(KnowledgeVersion::getVersion);
        
        List<KnowledgeVersion> versions = knowledgeVersionMapper.selectList(wrapper);
        return versions.stream().map(version -> {
            KnowledgeVersionDTO dto = new KnowledgeVersionDTO();
            BeanUtils.copyProperties(version, dto);
            return dto;
        }).collect(Collectors.toList());
    }
    
    @Override
    public KnowledgeVersionDTO getKnowledgeVersion(Long knowledgeId, Long version) {
        LambdaQueryWrapper<KnowledgeVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgeVersion::getVersion, version);
        
        KnowledgeVersion versionEntity = knowledgeVersionMapper.selectOne(wrapper);
        if (versionEntity == null) {
            return null;
        }
        
        KnowledgeVersionDTO dto = new KnowledgeVersionDTO();
        BeanUtils.copyProperties(versionEntity, dto);
        return dto;
    }
    
    @Override
    public KnowledgeVersionDTO.DiffResult compareVersions(Long knowledgeId, Long version1, Long version2) {
        KnowledgeVersionDTO v1 = getKnowledgeVersion(knowledgeId, version1);
        KnowledgeVersionDTO v2 = getKnowledgeVersion(knowledgeId, version2);
        
        if (v1 == null || v2 == null) {
            throw new RuntimeException("版本不存在");
        }
        
        // 使用DiffUtil比较内容
        List<DiffUtil.DiffLine> diffLines = DiffUtil.diff(
            v1.getContent() != null ? v1.getContent() : "",
            v2.getContent() != null ? v2.getContent() : ""
        );
        
        // 转换为DTO
        List<KnowledgeVersionDTO.DiffResult.DiffLine> diffLineDTOs = diffLines.stream().map(line -> {
            KnowledgeVersionDTO.DiffResult.DiffLine dto = new KnowledgeVersionDTO.DiffResult.DiffLine();
            dto.setType(line.getType().name());
            dto.setContent(line.getContent());
            dto.setLineNumber1(line.getLineNumber1());
            dto.setLineNumber2(line.getLineNumber2());
            return dto;
        }).collect(Collectors.toList());
        
        // 获取统计信息
        DiffUtil.DiffStats stats = DiffUtil.getStats(diffLines);
        KnowledgeVersionDTO.DiffResult.DiffStats statsDTO = new KnowledgeVersionDTO.DiffResult.DiffStats();
        statsDTO.setInsertCount(stats.getInsertCount());
        statsDTO.setDeleteCount(stats.getDeleteCount());
        statsDTO.setEqualCount(stats.getEqualCount());
        
        // 构建结果
        KnowledgeVersionDTO.DiffResult result = new KnowledgeVersionDTO.DiffResult();
        result.setKnowledgeId(knowledgeId);
        result.setVersion1(version1);
        result.setVersion2(version2);
        result.setDiffLines(diffLineDTOs);
        result.setStats(statsDTO);
        
        return result;
    }
}

