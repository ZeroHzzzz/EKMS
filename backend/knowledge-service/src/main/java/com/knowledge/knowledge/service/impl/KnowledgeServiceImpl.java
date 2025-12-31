package com.knowledge.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.KnowledgeQueryDTO;
import com.knowledge.api.dto.KnowledgeVersionDTO;
import com.knowledge.api.service.FileService;
import com.knowledge.api.service.KnowledgeService;
import com.knowledge.common.util.DiffUtil;
import com.knowledge.common.constant.Constants;
import com.knowledge.knowledge.entity.Knowledge;
import com.knowledge.knowledge.entity.KnowledgeVersion;
import com.knowledge.knowledge.mapper.KnowledgeMapper;
import com.knowledge.knowledge.mapper.KnowledgeVersionMapper;
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
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.core.util.IdUtil;

@Slf4j
@Service
@DubboService
public class KnowledgeServiceImpl implements KnowledgeService {

    @Resource
    private KnowledgeMapper knowledgeMapper;
    
    @Resource
    private KnowledgeVersionMapper knowledgeVersionMapper;
    
    @DubboReference(check = false, timeout = 10000)
    private FileService fileService;

    @Override
    @Transactional
    public KnowledgeDTO createKnowledge(KnowledgeDTO knowledgeDTO) {
        Knowledge knowledge = new Knowledge();
        BeanUtils.copyProperties(knowledgeDTO, knowledge);
        knowledge.setStatus(Constants.FILE_STATUS_DRAFT);
        knowledge.setClickCount(0L);
        knowledge.setCollectCount(0L);
        knowledge.setVersion(1L);
        knowledge.setCurrentBranch("main"); // 默认分支为main
        knowledge.setCreateTime(LocalDateTime.now());
        // 设置创建人
        if (knowledgeDTO.getCreateBy() != null) {
            knowledge.setCreateBy(knowledgeDTO.getCreateBy());
        }
        knowledgeMapper.insert(knowledge);
        
        // 创建初始commit（版本1）
        String commitMessage = "初始版本";
        String commitHash = generateCommitHash(knowledge, commitMessage, knowledgeDTO.getCreateBy(), "main", null);
        knowledge.setCurrentCommitHash(commitHash);
        knowledgeMapper.updateById(knowledge);
        
        // 保存初始版本到版本历史
        KnowledgeVersion initialVersion = new KnowledgeVersion();
        initialVersion.setKnowledgeId(knowledge.getId());
        initialVersion.setVersion(1L);
        initialVersion.setTitle(knowledge.getTitle());
        initialVersion.setContent(knowledge.getContent());
        initialVersion.setSummary(knowledge.getSummary());
        initialVersion.setCategory(knowledge.getCategory());
        initialVersion.setKeywords(knowledge.getKeywords());
        initialVersion.setAuthor(knowledge.getAuthor());
        initialVersion.setDepartment(knowledge.getDepartment());
        initialVersion.setFileId(knowledge.getFileId());
        initialVersion.setChangeDescription(commitMessage);
        initialVersion.setCommitMessage(commitMessage);
        initialVersion.setBranch("main");
        initialVersion.setParentCommitId(null);
        initialVersion.setCommitHash(commitHash);
        initialVersion.setCreatedBy(knowledgeDTO.getCreateBy());
        initialVersion.setCreateTime(LocalDateTime.now());
        knowledgeVersionMapper.insert(initialVersion);
        
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
        
        // 处理版本号，如果为null则初始化为1
        Long currentVersion = knowledge.getVersion();
        if (currentVersion == null) {
            currentVersion = 1L;
        }
        knowledge.setVersion(currentVersion + 1);
        
        // 设置当前分支（如果没有则默认为main）
        if (knowledge.getCurrentBranch() == null || knowledge.getCurrentBranch().trim().isEmpty()) {
            knowledge.setCurrentBranch("main");
        }
        
        knowledge.setUpdateTime(LocalDateTime.now());
        knowledgeMapper.updateById(knowledge);
        
        // 更新当前commit hash（从最新版本获取）
        LambdaQueryWrapper<KnowledgeVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledge.getId());
        versionWrapper.eq(KnowledgeVersion::getBranch, knowledge.getCurrentBranch());
        versionWrapper.orderByDesc(KnowledgeVersion::getVersion);
        versionWrapper.last("LIMIT 1");
        KnowledgeVersion latestVersion = knowledgeVersionMapper.selectOne(versionWrapper);
        if (latestVersion != null && latestVersion.getCommitHash() != null) {
            knowledge.setCurrentCommitHash(latestVersion.getCommitHash());
            knowledgeMapper.updateById(knowledge);
        }
        
        KnowledgeDTO result = new KnowledgeDTO();
        BeanUtils.copyProperties(knowledge, result);
        return result;
    }
    
    /**
     * 保存版本历史（Git风格：生成commit hash）
     */
    private void saveVersionHistory(Knowledge knowledge, String changeDescription, String updateBy) {
        KnowledgeVersion version = new KnowledgeVersion();
        version.setKnowledgeId(knowledge.getId());
        
        // 处理版本号，如果为null则初始化为1
        Long currentVersion = knowledge.getVersion();
        if (currentVersion == null) {
            currentVersion = 1L;
        }
        version.setVersion(currentVersion);
        
        // 复制知识内容
        version.setTitle(knowledge.getTitle());
        version.setContent(knowledge.getContent());
        version.setSummary(knowledge.getSummary());
        version.setCategory(knowledge.getCategory());
        version.setKeywords(knowledge.getKeywords());
        version.setAuthor(knowledge.getAuthor());
        version.setDepartment(knowledge.getDepartment());
        version.setFileId(knowledge.getFileId());
        
        // 设置变更说明和commit消息
        String commitMessage = changeDescription != null && !changeDescription.trim().isEmpty() 
            ? changeDescription 
            : "更新知识内容";
        version.setChangeDescription(commitMessage);
        version.setCommitMessage(commitMessage);
        
        // 设置创建信息
        version.setCreatedBy(updateBy != null ? updateBy : knowledge.getUpdateBy());
        version.setCreateTime(LocalDateTime.now());
        
        // 设置分支（从knowledge获取，默认为main）
        String branch = knowledge.getCurrentBranch() != null ? knowledge.getCurrentBranch() : "main";
        version.setBranch(branch);
        
        // 查找父commit（当前分支的最新commit）
        Long parentCommitId = findParentCommitId(knowledge.getId(), branch);
        version.setParentCommitId(parentCommitId);
        
        // 生成commit hash（类似Git的commit hash）
        String commitHash = generateCommitHash(knowledge, commitMessage, updateBy, branch, parentCommitId);
        version.setCommitHash(commitHash);
        
        knowledgeVersionMapper.insert(version);
        log.info("保存版本历史（Commit） - 知识ID: {}, 版本: {}, 分支: {}, Commit Hash: {}", 
                knowledge.getId(), currentVersion, branch, commitHash);
    }
    
    /**
     * 查找父commit ID（当前分支的最新commit）
     */
    private Long findParentCommitId(Long knowledgeId, String branch) {
        LambdaQueryWrapper<KnowledgeVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgeVersion::getBranch, branch);
        wrapper.orderByDesc(KnowledgeVersion::getVersion);
        wrapper.last("LIMIT 1");
        
        KnowledgeVersion latestVersion = knowledgeVersionMapper.selectOne(wrapper);
        return latestVersion != null ? latestVersion.getId() : null;
    }
    
    /**
     * 生成commit hash（类似Git commit hash）
     * 使用SHA256哈希：知识ID + 版本号 + 内容 + 变更说明 + 创建人 + 分支 + 父commit ID + 时间戳
     */
    private String generateCommitHash(Knowledge knowledge, String commitMessage, String updateBy, String branch, Long parentCommitId) {
        StringBuilder content = new StringBuilder();
        content.append("knowledgeId:").append(knowledge.getId());
        content.append("|version:").append(knowledge.getVersion());
        content.append("|title:").append(knowledge.getTitle() != null ? knowledge.getTitle() : "");
        content.append("|content:").append(knowledge.getContent() != null ? knowledge.getContent() : "");
        content.append("|message:").append(commitMessage);
        content.append("|author:").append(updateBy != null ? updateBy : "");
        content.append("|branch:").append(branch);
        content.append("|parent:").append(parentCommitId != null ? parentCommitId : "");
        content.append("|timestamp:").append(System.currentTimeMillis());
        
        // 使用SHA256生成hash（取前16位，类似Git的短hash）
        String fullHash = DigestUtil.sha256Hex(content.toString());
        return fullHash.substring(0, 16); // 取前16位作为commit hash
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
        // 先获取知识信息，以便删除关联的文件
        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            return false;
        }
        
        // 删除知识记录
        boolean deleted = knowledgeMapper.deleteById(id) > 0;
        
        if (deleted && knowledge.getFileId() != null) {
            // 检查该文件是否还被其他知识引用
            LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Knowledge::getFileId, knowledge.getFileId());
            wrapper.ne(Knowledge::getId, id); // 排除当前已删除的知识
            Long count = knowledgeMapper.selectCount(wrapper);
            
            // 如果没有其他知识引用该文件，则删除文件
            if (count == null || count == 0) {
                try {
                    fileService.deleteFile(knowledge.getFileId());
                    log.info("删除知识时同时删除了关联的文件: knowledgeId={}, fileId={}", id, knowledge.getFileId());
                } catch (Exception e) {
                    log.error("删除关联文件失败: knowledgeId={}, fileId={}", id, knowledge.getFileId(), e);
                    // 文件删除失败不影响知识删除的成功
                }
            } else {
                log.info("文件仍被其他知识引用，不删除文件: fileId={}, 引用数量={}", knowledge.getFileId(), count);
            }
        }
        
        return deleted;
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
        wrapper.orderByDesc(KnowledgeVersion::getCreateTime); // 按创建时间倒序（最新的在前）
        
        List<KnowledgeVersion> versions = knowledgeVersionMapper.selectList(wrapper);
        return versions.stream().map(version -> {
            KnowledgeVersionDTO dto = new KnowledgeVersionDTO();
            BeanUtils.copyProperties(version, dto);
            // 如果commitMessage为空，使用changeDescription
            if (dto.getCommitMessage() == null || dto.getCommitMessage().trim().isEmpty()) {
                dto.setCommitMessage(dto.getChangeDescription());
            }
            return dto;
        }).collect(Collectors.toList());
    }
    
    @Override
    public KnowledgeVersionDTO getKnowledgeVersion(Long knowledgeId, Long version) {
        // 先获取当前知识的分支信息
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        String currentBranch = (knowledge != null && knowledge.getCurrentBranch() != null) 
            ? knowledge.getCurrentBranch() 
            : "main";
        
        LambdaQueryWrapper<KnowledgeVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgeVersion::getVersion, version);
        wrapper.eq(KnowledgeVersion::getBranch, currentBranch);
        wrapper.orderByDesc(KnowledgeVersion::getCreateTime); // 按创建时间倒序，确保取最新的
        wrapper.last("LIMIT 1"); // 限制只返回一条记录
        
        // 使用 selectList 然后取第一条，避免 selectOne 在多条记录时抛异常
        List<KnowledgeVersion> versions = knowledgeVersionMapper.selectList(wrapper);
        KnowledgeVersion versionEntity = (versions != null && !versions.isEmpty()) ? versions.get(0) : null;
        
        // 如果按分支查询没有结果，尝试不按分支查询（兼容旧数据）
        if (versionEntity == null) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
            wrapper.eq(KnowledgeVersion::getVersion, version);
            wrapper.orderByDesc(KnowledgeVersion::getCreateTime);
            wrapper.last("LIMIT 1");
            // 使用 selectList 然后取第一条，避免 selectOne 在多条记录时抛异常
            List<KnowledgeVersion> fallbackVersions = knowledgeVersionMapper.selectList(wrapper);
            if (fallbackVersions != null && !fallbackVersions.isEmpty()) {
                versionEntity = fallbackVersions.get(0);
            }
        }
        
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

