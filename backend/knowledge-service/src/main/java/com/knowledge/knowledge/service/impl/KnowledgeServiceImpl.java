package com.knowledge.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.api.dto.DepartmentDTO;
import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.KnowledgeQueryDTO;
import com.knowledge.api.dto.KnowledgeVersionDTO;
import com.knowledge.api.dto.StatisticsDTO;
import com.knowledge.api.dto.UserDTO;
import com.knowledge.api.service.DepartmentService;
import com.knowledge.api.service.FileService;
import com.knowledge.api.service.KnowledgeService;
import com.knowledge.api.service.SearchService;
import com.knowledge.api.service.UserService;
import com.knowledge.common.util.DiffUtil;
import com.knowledge.common.constant.Constants;
import com.knowledge.knowledge.entity.Knowledge;
import com.knowledge.knowledge.entity.KnowledgeVersion;
import com.knowledge.knowledge.entity.UserKnowledgeCollection;
import com.knowledge.knowledge.mapper.KnowledgeMapper;
import com.knowledge.knowledge.mapper.KnowledgeVersionMapper;
import com.knowledge.knowledge.mapper.UserKnowledgeCollectionMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    
    @Resource
    private UserKnowledgeCollectionMapper collectionMapper;
    
    @DubboReference(check = false, timeout = 10000)
    private FileService fileService;

    @DubboReference(check = false, timeout = 10000)
    private SearchService searchService;

    @DubboReference(check = false, timeout = 10000)
    private UserService userService;
    
    @DubboReference(check = false, timeout = 10000)
    private DepartmentService departmentService;

    @Override
    @Transactional
    public KnowledgeDTO createKnowledge(KnowledgeDTO knowledgeDTO) {
        Knowledge knowledge = new Knowledge();
        BeanUtils.copyProperties(knowledgeDTO, knowledge);
        
        // 检查创建者是否是管理员
        boolean isAdmin = false;
        String status = Constants.FILE_STATUS_PENDING;
        try {
            if (knowledgeDTO.getCreateBy() != null) {
                UserDTO user = userService.getUserByUsername(knowledgeDTO.getCreateBy());
                if (user != null && Constants.ROLE_ADMIN.equals(user.getRole())) {
                    isAdmin = true;
                    if (knowledgeDTO.getStatus() != null && !knowledgeDTO.getStatus().isEmpty()) {
                        status = knowledgeDTO.getStatus();
                    } else {
                        status = Constants.FILE_STATUS_APPROVED;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("检查用户角色失败，使用默认状态PENDING: {}", e.getMessage());
        }
        
        knowledge.setStatus(status);
        knowledge.setClickCount(0L);
        knowledge.setCollectCount(0L);
        knowledge.setVersion(1L);
        knowledge.setHasDraft(false);
        knowledge.setCurrentBranch("main");
        knowledge.setCreateTime(LocalDateTime.now());
        
        // 如果是管理员创建且状态为已发布，设置publishedVersion
        if (isAdmin && Constants.FILE_STATUS_APPROVED.equals(status)) {
            knowledge.setPublishedVersion(1L);
        }
        
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
        // 设置版本状态
        initialVersion.setStatus(status);
        initialVersion.setIsPublished(isAdmin && Constants.FILE_STATUS_APPROVED.equals(status));
        knowledgeVersionMapper.insert(initialVersion);
        
        // 提取并保存文档全文内容
        if (knowledge.getFileId() != null) {
            String fullText = extractTextFromFile(knowledge.getFileId());
            if (fullText != null) {
                knowledge.setContentText(fullText);
                knowledgeMapper.updateById(knowledge);
            }
        }
        
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
        
        // 记录原始状态
        String originalStatus = knowledge.getStatus();
        Long originalPublishedVersion = knowledge.getPublishedVersion();
        
        // 检查更新者是否是管理员
        boolean isAdmin = false;
        try {
            if (knowledgeDTO.getUpdateBy() != null) {
                UserDTO user = userService.getUserByUsername(knowledgeDTO.getUpdateBy());
                isAdmin = user != null && Constants.ROLE_ADMIN.equals(user.getRole());
            }
        } catch (Exception e) {
            log.warn("检查用户角色失败: {}", e.getMessage());
        }
        
        // 处理版本号
        Long currentVersion = knowledge.getVersion();
        if (currentVersion == null) {
            currentVersion = 1L;
        }
        Long newVersion = currentVersion + 1;
        
        // 保存新版本到版本历史表（作为草稿/待审核）
        String versionStatus;
        boolean isPublished = false;
        
        // 判断新版本的状态
        if (Constants.FILE_STATUS_APPROVED.equals(originalStatus) && originalPublishedVersion != null) {
            // 已有发布版本，新编辑的作为待审核草稿
            if (isAdmin) {
                // 管理员编辑：直接发布新版本
                versionStatus = Constants.FILE_STATUS_APPROVED;
                isPublished = true;
                knowledge.setPublishedVersion(newVersion);
                knowledge.setHasDraft(false);
                knowledge.setStatus(Constants.FILE_STATUS_APPROVED);
                log.info("管理员编辑已发布文章，直接发布新版本 - 知识ID: {}, 新版本: {}", knowledge.getId(), newVersion);
            } else {
                // 普通用户编辑：创建待审核草稿，保留已发布版本
                versionStatus = Constants.FILE_STATUS_PENDING;
                isPublished = false;
                knowledge.setHasDraft(true);
                // status保持APPROVED，因为已发布版本仍然对外可见
                // 但同时标记有待审核的草稿
                log.info("用户编辑已发布文章，创建待审核草稿 - 知识ID: {}, 草稿版本: {}, 已发布版本: {}", 
                        knowledge.getId(), newVersion, originalPublishedVersion);
            }
        } else {
            // 尚未有发布版本，或原状态不是已发布
            if (isAdmin) {
                versionStatus = Constants.FILE_STATUS_APPROVED;
                isPublished = true;
                knowledge.setPublishedVersion(newVersion);
                knowledge.setHasDraft(false);
                knowledge.setStatus(Constants.FILE_STATUS_APPROVED);
            } else {
                versionStatus = Constants.FILE_STATUS_PENDING;
                isPublished = false;
                knowledge.setHasDraft(true);
                knowledge.setStatus(Constants.FILE_STATUS_PENDING);
            }
        }
        
        // 保存版本历史
        saveVersionHistoryWithStatus(knowledge, knowledgeDTO, newVersion, versionStatus, isPublished);
        
        // 更新知识内容（保存最新草稿/发布内容）
        BeanUtils.copyProperties(knowledgeDTO, knowledge, "id", "createTime", "createBy", "contentText", 
                "publishedVersion", "hasDraft", "status", "version");
        
        // 如果文件ID变更或contentText为空，重新提取内容
        if (knowledge.getFileId() != null) {
            Knowledge originalKnowledge = knowledgeMapper.selectById(knowledgeDTO.getId());
            if (knowledge.getContentText() == null || 
                !knowledge.getFileId().equals(originalKnowledge.getFileId())) {
                String fullText = extractTextFromFile(knowledge.getFileId());
                if (fullText != null) {
                    knowledge.setContentText(fullText);
                }
            }
        }
        
        knowledge.setVersion(newVersion);
        
        // 设置当前分支（如果没有则默认为main）
        if (knowledge.getCurrentBranch() == null || knowledge.getCurrentBranch().trim().isEmpty()) {
            knowledge.setCurrentBranch("main");
        }
        
        knowledge.setUpdateTime(LocalDateTime.now());
        knowledgeMapper.updateById(knowledge);
        
        // 更新当前commit hash
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
     * 保存版本历史（带状态）
     */
    private void saveVersionHistoryWithStatus(Knowledge knowledge, KnowledgeDTO dto, Long newVersion, 
                                              String versionStatus, boolean isPublished) {
        KnowledgeVersion version = new KnowledgeVersion();
        version.setKnowledgeId(knowledge.getId());
        version.setVersion(newVersion);
        
        // 从DTO获取新内容
        version.setTitle(dto.getTitle() != null ? dto.getTitle() : knowledge.getTitle());
        version.setContent(dto.getContent() != null ? dto.getContent() : knowledge.getContent());
        version.setSummary(dto.getSummary() != null ? dto.getSummary() : knowledge.getSummary());
        version.setCategory(dto.getCategory() != null ? dto.getCategory() : knowledge.getCategory());
        version.setKeywords(dto.getKeywords() != null ? dto.getKeywords() : knowledge.getKeywords());
        version.setAuthor(dto.getAuthor() != null ? dto.getAuthor() : knowledge.getAuthor());
        version.setDepartment(dto.getDepartment() != null ? dto.getDepartment() : knowledge.getDepartment());
        version.setFileId(dto.getFileId() != null ? dto.getFileId() : knowledge.getFileId());
        
        String commitMessage = dto.getChangeDescription() != null && !dto.getChangeDescription().trim().isEmpty() 
            ? dto.getChangeDescription() 
            : "更新知识内容";
        version.setChangeDescription(commitMessage);
        version.setCommitMessage(commitMessage);
        
        version.setCreatedBy(dto.getUpdateBy() != null ? dto.getUpdateBy() : knowledge.getUpdateBy());
        version.setCreateTime(LocalDateTime.now());
        
        String branch = knowledge.getCurrentBranch() != null ? knowledge.getCurrentBranch() : "main";
        version.setBranch(branch);
        
        Long parentCommitId = findParentCommitId(knowledge.getId(), branch);
        version.setParentCommitId(parentCommitId);
        
        // 临时设置版本号用于生成hash
        knowledge.setVersion(newVersion);
        String commitHash = generateCommitHash(knowledge, commitMessage, dto.getUpdateBy(), branch, parentCommitId);
        version.setCommitHash(commitHash);
        
        // 设置版本状态
        version.setStatus(versionStatus);
        version.setIsPublished(isPublished);
        
        knowledgeVersionMapper.insert(version);
        log.info("保存版本历史 - 知识ID: {}, 版本: {}, 状态: {}, 是否发布: {}", 
                knowledge.getId(), newVersion, versionStatus, isPublished);
    }
    
    /**
     * 安全比较两个对象是否相等（处理null情况）
     */
    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
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
        
        // 根据includeFolders参数决定是否过滤文件夹
        // 默认为false，保持向后兼容（只查询文件）
        boolean includeFolders = "true".equalsIgnoreCase(queryDTO.getIncludeFolders());
        if (!includeFolders) {
            wrapper.isNotNull(Knowledge::getFileId);
        }
        // 如果includeFolders为true，则不过滤，返回所有知识（包括文件和文件夹）
        
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
    @Transactional
    public boolean batchUpdateKnowledge(List<Long> knowledgeIds, Map<String, Object> updateData) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return false;
        }
        
        if (updateData == null || updateData.isEmpty()) {
            return false;
        }
        
        for (Long id : knowledgeIds) {
            Knowledge knowledge = knowledgeMapper.selectById(id);
            if (knowledge == null) {
                continue;
            }
            
            // 更新字段
            if (updateData.containsKey("category")) {
                knowledge.setCategory((String) updateData.get("category"));
            }
            if (updateData.containsKey("status")) {
                knowledge.setStatus((String) updateData.get("status"));
            }
            if (updateData.containsKey("keywords")) {
                knowledge.setKeywords((String) updateData.get("keywords"));
            }
            if (updateData.containsKey("department")) {
                knowledge.setDepartment((String) updateData.get("department"));
            }
            
            knowledge.setUpdateTime(LocalDateTime.now());
            knowledgeMapper.updateById(knowledge);
            
            // 同步更新Elasticsearch索引
            try {
                KnowledgeDTO dto = new KnowledgeDTO();
                BeanUtils.copyProperties(knowledge, dto);
                searchService.updateIndex(dto);
            } catch (Exception e) {
                log.warn("同步更新索引失败: knowledgeId={}", id, e);
            }
        }
        
        return true;
    }
    
    /**
     * 从文件中提取文本内容
     */
    private String extractTextFromFile(Long fileId) {
        try {
            String filePath = fileService.getFilePath(fileId);
            if (filePath == null) {
                log.warn("文件路径不存在: fileId={}", fileId);
                return null;
            }
            
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                log.warn("物理文件不存在: path={}", filePath);
                return null;
            }
            
            org.apache.tika.Tika tika = new org.apache.tika.Tika();
            // 设置最大字符串长度，防止内存溢出 (-1为不限制，但建议设置合理的限制)
            tika.setMaxStringLength(10 * 1024 * 1024); // 10MB
            
            String text = tika.parseToString(file);
            log.info("提取文本成功: fileId={}, length={}", fileId, text.length());
            return text;
        } catch (Exception e) {
            log.error("提取文件文本失败: fileId={}", fileId, e);
            return null;
        }
    }

    @Override
    @Transactional
    public boolean publishKnowledge(Long id) {
        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            return false;
        }
        
        // 获取当前最新版本号
        Long currentVersion = knowledge.getVersion();
        if (currentVersion == null) {
            currentVersion = 1L;
        }
        
        // 更新knowledge表：发布当前版本
        knowledge.setStatus(Constants.FILE_STATUS_APPROVED);
        knowledge.setPublishedVersion(currentVersion);  // 设置发布版本为当前版本
        knowledge.setHasDraft(false);  // 清除草稿标记
        knowledge.setUpdateTime(LocalDateTime.now());
        boolean result = knowledgeMapper.updateById(knowledge) > 0;
        
        // 更新version表：标记当前版本为已发布
        if (result) {
            LambdaQueryWrapper<KnowledgeVersion> versionWrapper = new LambdaQueryWrapper<>();
            versionWrapper.eq(KnowledgeVersion::getKnowledgeId, id);
            versionWrapper.eq(KnowledgeVersion::getVersion, currentVersion);
            KnowledgeVersion latestVersion = knowledgeVersionMapper.selectOne(versionWrapper);
            if (latestVersion != null) {
                latestVersion.setIsPublished(true);
                latestVersion.setStatus(Constants.FILE_STATUS_APPROVED);
                knowledgeVersionMapper.updateById(latestVersion);
                log.info("版本已标记为发布: knowledgeId={}, version={}", id, currentVersion);
            }
            
            // 将之前的发布版本标记为非发布（可选，保留历史）
            LambdaQueryWrapper<KnowledgeVersion> oldVersionWrapper = new LambdaQueryWrapper<>();
            oldVersionWrapper.eq(KnowledgeVersion::getKnowledgeId, id);
            oldVersionWrapper.ne(KnowledgeVersion::getVersion, currentVersion);
            oldVersionWrapper.eq(KnowledgeVersion::getIsPublished, true);
            List<KnowledgeVersion> oldPublishedVersions = knowledgeVersionMapper.selectList(oldVersionWrapper);
            for (KnowledgeVersion oldVersion : oldPublishedVersions) {
                oldVersion.setIsPublished(false);
                knowledgeVersionMapper.updateById(oldVersion);
            }
        }
        
        // 同步更新Elasticsearch索引
        if (result) {
            try {
                KnowledgeDTO dto = new KnowledgeDTO();
                BeanUtils.copyProperties(knowledge, dto);
                searchService.updateIndex(dto);
                log.info("发布知识并更新索引成功: knowledgeId={}, publishedVersion={}", id, currentVersion);
            } catch (Exception e) {
                log.warn("发布知识后更新索引失败: knowledgeId={}", id, e);
                // 索引失败不影响主流程
            }
        }
        
        return result;
    }

    @Override
    public List<KnowledgeDTO> getHotKnowledge(int limit) {
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        // 不限制状态，显示所有知识（包括草稿、已发布等），按点击量排序
        // 如果只想显示已发布的，可以取消下面的注释
        // wrapper.eq(Knowledge::getStatus, Constants.FILE_STATUS_APPROVED);

        // 排除文件夹（文件夹没有文件ID，不应参与热点排序）
        wrapper.isNotNull(Knowledge::getFileId);
        
        // 按点击量降序排序，如果点击量为null则按0处理
        wrapper.orderByDesc(Knowledge::getClickCount);
        wrapper.orderByDesc(Knowledge::getCreateTime); // 如果点击量相同，按创建时间排序
        wrapper.last("LIMIT " + limit);
        
        List<Knowledge> knowledges = knowledgeMapper.selectList(wrapper);
        return knowledges.stream().map(knowledge -> {
            KnowledgeDTO dto = new KnowledgeDTO();
            BeanUtils.copyProperties(knowledge, dto);
            // 确保点击量和收藏量不为null
            if (dto.getClickCount() == null) {
                dto.setClickCount(0L);
            }
            if (dto.getCollectCount() == null) {
                dto.setCollectCount(0L);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeDTO> getRelatedKnowledge(Long id, int limit) {
        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            return List.of();
        }
        
        List<KnowledgeDTO> result = new ArrayList<>();
        
        // 1. 尝试使用搜索引擎查找相关内容（基于关键词和标题）
        try {
            com.knowledge.api.dto.SearchRequestDTO searchRequest = new com.knowledge.api.dto.SearchRequestDTO();
            // 构建搜索关键词：优先使用定义的关键词，其次使用标题
            StringBuilder searchKeywords = new StringBuilder();
            if (knowledge.getKeywords() != null && !knowledge.getKeywords().trim().isEmpty()) {
                // 将逗号分隔的关键词转换为空格分隔，以便搜索引擎处理
                searchKeywords.append(knowledge.getKeywords().replace(",", " ").replace("，", " "));
            } else {
                // 如果没有关键词，使用标题进行搜索
                searchKeywords.append(knowledge.getTitle());
            }
            
            searchRequest.setKeyword(searchKeywords.toString());
            // 限制搜索范围为同分类（可选，根据需求决定是否限制）
            searchRequest.setCategory(knowledge.getCategory());
            searchRequest.setPageNum(1);
            searchRequest.setPageSize(limit + 5); // 多取一些以便过滤掉自身
            searchRequest.setStatus(Constants.FILE_STATUS_APPROVED); // 只推荐已发布的
            
            com.knowledge.api.dto.SearchResultDTO searchResult = searchService.search(searchRequest);
            if (searchResult != null && searchResult.getResults() != null) {
                // 过滤掉自身
                result = searchResult.getResults().stream()
                    .filter(k -> !k.getId().equals(id))
                    .limit(limit)
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("使用搜索引擎获取相关知识失败，降级为数据库查询: knowledgeId={}, error={}", id, e.getMessage());
        }
        
        // 2. 如果搜索引擎没有返回结果（或失败），使用数据库基于分类查询（兜底方案）
        if (result.isEmpty()) {
            LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
            // 只要同分类的
            if (knowledge.getCategory() != null) {
                wrapper.eq(Knowledge::getCategory, knowledge.getCategory());
            }
            wrapper.ne(Knowledge::getId, id); // 排除自身
            wrapper.eq(Knowledge::getStatus, Constants.FILE_STATUS_APPROVED);
            // 优先按点击量排序
            wrapper.orderByDesc(Knowledge::getClickCount);
            wrapper.last("LIMIT " + limit);
            
            List<Knowledge> knowledges = knowledgeMapper.selectList(wrapper);
            result = knowledges.stream().map(k -> {
                KnowledgeDTO dto = new KnowledgeDTO();
                BeanUtils.copyProperties(k, dto);
                return dto;
            }).collect(Collectors.toList());
        }
        
        return result;
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

    @Override
    public StatisticsDTO getStatistics() {
        StatisticsDTO statistics = new StatisticsDTO();
        
        // 统计知识总数
        Long totalKnowledge = knowledgeMapper.selectCount(null);
        statistics.setTotalKnowledge(totalKnowledge != null ? totalKnowledge : 0L);
        
        // 统计总点击量
        Long totalClicks = knowledgeMapper.getTotalClicks();
        statistics.setTotalClicks(totalClicks != null ? totalClicks : 0L);
        
        // 统计总收藏量
        Long totalCollections = knowledgeMapper.getTotalCollections();
        statistics.setTotalCollections(totalCollections != null ? totalCollections : 0L);
        
        // 统计待审核数量（status = 'PENDING'）
        LambdaQueryWrapper<Knowledge> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Knowledge::getStatus, Constants.FILE_STATUS_PENDING);
        Long pendingAudit = knowledgeMapper.selectCount(pendingWrapper);
        statistics.setPendingAudit(pendingAudit != null ? pendingAudit : 0L);
        
        // 计算平均点击率和收藏率
        if (totalKnowledge != null && totalKnowledge > 0) {
            statistics.setAverageClickRate(totalClicks != null ? (double) totalClicks / totalKnowledge : 0.0);
            statistics.setAverageCollectRate(totalCollections != null ? (double) totalCollections / totalKnowledge : 0.0);
        } else {
            statistics.setAverageClickRate(0.0);
            statistics.setAverageCollectRate(0.0);
        }
        
        // 计算收藏点击比
        if (totalClicks != null && totalClicks > 0) {
            statistics.setCollectClickRatio(totalCollections != null ? (double) totalCollections / totalClicks : 0.0);
        } else {
            statistics.setCollectClickRatio(0.0);
        }
        
        // 分类统计
        List<Map<String, Object>> categoryStatsData = knowledgeMapper.getCategoryStats();
        List<StatisticsDTO.CategoryStatDTO> categoryStats = categoryStatsData.stream().map(map -> {
            StatisticsDTO.CategoryStatDTO stat = new StatisticsDTO.CategoryStatDTO();
            stat.setCategory((String) map.get("category"));
            stat.setCount(((Number) map.get("count")).longValue());
            stat.setClicks(((Number) map.get("clicks")).longValue());
            stat.setCollections(((Number) map.get("collections")).longValue());
            return stat;
        }).collect(Collectors.toList());
        statistics.setCategoryStats(categoryStats);
        
        // 点击量趋势（最近7天）
        List<Map<String, Object>> clickTrendData = knowledgeMapper.getClickTrend();
        List<StatisticsDTO.TrendDataDTO> clickTrend = clickTrendData.stream().map(map -> {
            StatisticsDTO.TrendDataDTO trend = new StatisticsDTO.TrendDataDTO();
            trend.setDate(map.get("date").toString());
            trend.setValue(((Number) map.get("value")).longValue());
            return trend;
        }).collect(Collectors.toList());
        statistics.setClickTrend(clickTrend);
        
        // 收藏量趋势（最近7天）
        List<Map<String, Object>> collectTrendData = knowledgeMapper.getCollectTrend();
        List<StatisticsDTO.TrendDataDTO> collectTrend = collectTrendData.stream().map(map -> {
            StatisticsDTO.TrendDataDTO trend = new StatisticsDTO.TrendDataDTO();
            trend.setDate(map.get("date").toString());
            trend.setValue(((Number) map.get("value")).longValue());
            return trend;
        }).collect(Collectors.toList());
        statistics.setCollectTrend(collectTrend);
        
        // 热门知识（Top 10）
        List<KnowledgeDTO> hotKnowledge = getHotKnowledge(10);
        statistics.setHotKnowledge(hotKnowledge);
        
        return statistics;
    }

    @Override
    @Transactional
    public boolean collectKnowledge(Long userId, Long knowledgeId) {
        // 检查是否已收藏
        LambdaQueryWrapper<UserKnowledgeCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserKnowledgeCollection::getUserId, userId);
        wrapper.eq(UserKnowledgeCollection::getKnowledgeId, knowledgeId);
        UserKnowledgeCollection existing = collectionMapper.selectOne(wrapper);
        
        if (existing != null) {
            // 已收藏，返回true
            return true;
        }
        
        // 添加收藏关系
        UserKnowledgeCollection collection = new UserKnowledgeCollection();
        collection.setUserId(userId);
        collection.setKnowledgeId(knowledgeId);
        collection.setCreateTime(LocalDateTime.now());
        int result = collectionMapper.insert(collection);
        
        // 更新知识的收藏数
        if (result > 0) {
            knowledgeMapper.updateCollectCount(knowledgeId, 1);
        }
        
        return result > 0;
    }

    @Override
    @Transactional
    public boolean cancelCollectKnowledge(Long userId, Long knowledgeId) {
        // 删除收藏关系
        LambdaQueryWrapper<UserKnowledgeCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserKnowledgeCollection::getUserId, userId);
        wrapper.eq(UserKnowledgeCollection::getKnowledgeId, knowledgeId);
        int result = collectionMapper.delete(wrapper);
        
        // 更新知识的收藏数
        if (result > 0) {
            knowledgeMapper.updateCollectCount(knowledgeId, -1);
        }
        
        return result > 0;
    }

    @Override
    public boolean isCollected(Long userId, Long knowledgeId) {
        LambdaQueryWrapper<UserKnowledgeCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserKnowledgeCollection::getUserId, userId);
        wrapper.eq(UserKnowledgeCollection::getKnowledgeId, knowledgeId);
        return collectionMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<KnowledgeDTO> getMyCollections(Long userId) {
        // 查询用户收藏的知识ID列表
        LambdaQueryWrapper<UserKnowledgeCollection> collectionWrapper = new LambdaQueryWrapper<>();
        collectionWrapper.eq(UserKnowledgeCollection::getUserId, userId);
        collectionWrapper.orderByDesc(UserKnowledgeCollection::getCreateTime);
        List<UserKnowledgeCollection> collections = collectionMapper.selectList(collectionWrapper);
        
        if (collections.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 提取知识ID列表
        List<Long> knowledgeIds = collections.stream()
            .map(UserKnowledgeCollection::getKnowledgeId)
            .collect(Collectors.toList());
        
        // 查询知识详情
        List<Knowledge> knowledges = knowledgeMapper.selectBatchIds(knowledgeIds);
        
        // 保持收藏顺序（按收藏时间倒序）
        Map<Long, Knowledge> knowledgeMap = knowledges.stream()
            .collect(Collectors.toMap(Knowledge::getId, k -> k));
        
        return knowledgeIds.stream()
            .map(knowledgeMap::get)
            .filter(k -> k != null)
            .map(knowledge -> {
                KnowledgeDTO dto = new KnowledgeDTO();
                BeanUtils.copyProperties(knowledge, dto);
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeDTO> getKnowledgeTree() {
        // 获取所有部门（使用DepartmentService）
        List<DepartmentDTO> departmentList = departmentService.getAllDepartments();
        List<String> departments = departmentList.stream()
            .map(DepartmentDTO::getName)
            .collect(Collectors.toList());
        
        // 知识结构显示已发布的知识，以及待审核的文件夹（以便用户能看到新创建的文件夹）
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Knowledge::getStatus, Constants.FILE_STATUS_APPROVED)
            .or(orW -> orW.isNull(Knowledge::getFileId).eq(Knowledge::getStatus, Constants.FILE_STATUS_PENDING)));
        wrapper.orderByAsc(Knowledge::getSortOrder);
        wrapper.orderByDesc(Knowledge::getCreateTime);
        
        List<Knowledge> knowledges = knowledgeMapper.selectList(wrapper);
        List<KnowledgeDTO> knowledgeDTOs = knowledges.stream().map(k -> {
            KnowledgeDTO dto = new KnowledgeDTO();
            BeanUtils.copyProperties(k, dto);
            return dto;
        }).collect(Collectors.toList());
        
        // 按部门组织知识树：部门作为根节点
        List<KnowledgeDTO> result = new ArrayList<>();
        
        // 为每个部门创建虚拟根节点（使用唯一的负数ID）
        long deptIdCounter = -1L;
        for (String dept : departments) {
            KnowledgeDTO deptNode = new KnowledgeDTO();
            deptNode.setId(deptIdCounter--); // 使用递减的负数ID，确保每个部门有唯一ID
            deptNode.setTitle(dept);
            deptNode.setDepartment(dept);
            deptNode.setIsDepartmentRoot(true); // 标记为部门根节点
            deptNode.setParentId(null);
            
            // 获取该部门下的所有知识（parentId为null的，即顶级知识）
            List<KnowledgeDTO> deptKnowledges = knowledgeDTOs.stream()
                .filter(k -> dept.equals(k.getDepartment()) && k.getParentId() == null)
                .sorted((a, b) -> {
                    int sortCompare = Integer.compare(a.getSortOrder() != null ? a.getSortOrder() : 0,
                                                     b.getSortOrder() != null ? b.getSortOrder() : 0);
                    if (sortCompare != 0) return sortCompare;
                    return b.getCreateTime().compareTo(a.getCreateTime());
                })
                .collect(Collectors.toList());
            
            // 构建该部门下的知识树结构
            deptKnowledges = buildTreeForDepartment(deptKnowledges, knowledgeDTOs);
            
            // 如果部门有知识，或者即使没有知识也显示部门节点
            result.add(deptNode);
            result.addAll(deptKnowledges);
        }
        
        // 处理没有部门的知识（department为null、空或"未知"）
        List<KnowledgeDTO> noDeptKnowledges = knowledgeDTOs.stream()
            .filter(k -> (k.getDepartment() == null || k.getDepartment().trim().isEmpty() || "未知".equals(k.getDepartment().trim())) && k.getParentId() == null)
            .sorted((a, b) -> {
                int sortCompare = Integer.compare(a.getSortOrder() != null ? a.getSortOrder() : 0,
                                                 b.getSortOrder() != null ? b.getSortOrder() : 0);
                if (sortCompare != 0) return sortCompare;
                return b.getCreateTime().compareTo(a.getCreateTime());
            })
            .collect(Collectors.toList());
        
        // 始终创建"未分类"部门节点（使用一个很大的负数ID避免与部门ID冲突）
        // 这样用户可以将没有分类的知识放到这里
        KnowledgeDTO unclassifiedNode = new KnowledgeDTO();
        unclassifiedNode.setId(-999999L);
        unclassifiedNode.setTitle("未分类");
        unclassifiedNode.setIsDepartmentRoot(true);
        unclassifiedNode.setParentId(null);
        result.add(unclassifiedNode);
        
        if (!noDeptKnowledges.isEmpty()) {
            List<KnowledgeDTO> unclassifiedTree = buildTreeForDepartment(noDeptKnowledges, knowledgeDTOs);
            result.addAll(unclassifiedTree);
        }
        
        return result;
    }
    
    /**
     * 为部门构建知识树结构
     */
    private List<KnowledgeDTO> buildTreeForDepartment(List<KnowledgeDTO> rootKnowledges, List<KnowledgeDTO> allKnowledges) {
        List<KnowledgeDTO> result = new ArrayList<>();
        Map<Long, KnowledgeDTO> knowledgeMap = allKnowledges.stream()
            .collect(Collectors.toMap(KnowledgeDTO::getId, k -> k));
        
        for (KnowledgeDTO root : rootKnowledges) {
            result.add(root);
            // 递归添加子节点
            addChildren(root, knowledgeMap, result);
        }
        
        return result;
    }
    
    /**
     * 递归添加子节点
     */
    private void addChildren(KnowledgeDTO parent, Map<Long, KnowledgeDTO> knowledgeMap, List<KnowledgeDTO> result) {
        List<KnowledgeDTO> children = knowledgeMap.values().stream()
            .filter(k -> parent.getId().equals(k.getParentId()))
            .sorted((a, b) -> {
                int sortCompare = Integer.compare(a.getSortOrder() != null ? a.getSortOrder() : 0,
                                                 b.getSortOrder() != null ? b.getSortOrder() : 0);
                if (sortCompare != 0) return sortCompare;
                return b.getCreateTime().compareTo(a.getCreateTime());
            })
            .collect(Collectors.toList());
        
        for (KnowledgeDTO child : children) {
            result.add(child);
            addChildren(child, knowledgeMap, result);
        }
    }

    @Override
    @Transactional
    public boolean moveKnowledge(Long knowledgeId, Long parentId, Integer sortOrder) {
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null) {
            return false;
        }
        
        // 检查不能移动到自己或自己的子节点
        if (parentId != null && parentId.equals(knowledgeId)) {
            throw new RuntimeException("不能移动到自己");
        }
        
        // 检查是否是自己的子节点（简化检查，实际应该递归检查）
        if (parentId != null) {
            Knowledge parent = knowledgeMapper.selectById(parentId);
            if (parent != null && parent.getParentId() != null && parent.getParentId().equals(knowledgeId)) {
                throw new RuntimeException("不能移动到自己的子节点");
            }
        }
        
        knowledge.setParentId(parentId);
        knowledge.setSortOrder(sortOrder != null ? sortOrder : 0);
        knowledge.setUpdateTime(LocalDateTime.now());
        
        knowledgeMapper.updateById(knowledge);
        
        // 同步更新Elasticsearch索引
        try {
            KnowledgeDTO dto = new KnowledgeDTO();
            BeanUtils.copyProperties(knowledge, dto);
            searchService.updateIndex(dto);
        } catch (Exception e) {
            log.warn("同步更新索引失败: knowledgeId={}", knowledgeId, e);
        }
        
        return true;
    }

    @Override
    public List<KnowledgeDTO> getKnowledgePath(Long knowledgeId) {
        List<KnowledgeDTO> path = new ArrayList<>();
        Knowledge current = knowledgeMapper.selectById(knowledgeId);
        
        if (current == null) {
            return path;
        }
        
        // 从当前节点向上追溯到根节点
        while (current != null) {
            KnowledgeDTO dto = new KnowledgeDTO();
            BeanUtils.copyProperties(current, dto);
            path.add(0, dto); // 插入到列表开头，保持从根到当前的顺序
            
            if (current.getParentId() != null) {
                current = knowledgeMapper.selectById(current.getParentId());
            } else {
                current = null;
            }
        }
        
        return path;
    }

    @Override
    public List<KnowledgeDTO> getChildren(Long parentId) {
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        if (parentId == null) {
            wrapper.isNull(Knowledge::getParentId);
        } else {
            wrapper.eq(Knowledge::getParentId, parentId);
        }
        wrapper.orderByAsc(Knowledge::getSortOrder);
        wrapper.orderByDesc(Knowledge::getCreateTime);
        
        List<Knowledge> children = knowledgeMapper.selectList(wrapper);
        return children.stream().map(k -> {
            KnowledgeDTO dto = new KnowledgeDTO();
            BeanUtils.copyProperties(k, dto);
            return dto;
        }).collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public KnowledgeDTO revertToVersion(Long knowledgeId, Long targetVersion, String operatorUsername) {
        // 1. 获取当前知识
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null) {
            throw new RuntimeException("知识不存在");
        }
        
        // 2. 检查操作者是否是管理员（只有管理员才能回退版本）
        boolean isAdmin = false;
        try {
            if (operatorUsername != null) {
                UserDTO user = userService.getUserByUsername(operatorUsername);
                isAdmin = user != null && Constants.ROLE_ADMIN.equals(user.getRole());
            }
        } catch (Exception e) {
            log.warn("检查用户角色失败: {}", e.getMessage());
        }
        
        if (!isAdmin) {
            throw new RuntimeException("只有管理员才能执行版本回退操作");
        }
        
        // 3. 获取目标版本的内容
        LambdaQueryWrapper<KnowledgeVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgeVersion::getVersion, targetVersion);
        wrapper.last("LIMIT 1");
        KnowledgeVersion targetVersionEntity = knowledgeVersionMapper.selectOne(wrapper);
        
        if (targetVersionEntity == null) {
            throw new RuntimeException("目标版本不存在");
        }
        
        // 4. 创建新版本（回退版本）
        Long newVersion = knowledge.getVersion() + 1;
        String commitMessage = "回退到版本 v" + targetVersion;
        
        KnowledgeVersion revertVersion = new KnowledgeVersion();
        revertVersion.setKnowledgeId(knowledgeId);
        revertVersion.setVersion(newVersion);
        // 复制目标版本的内容
        revertVersion.setTitle(targetVersionEntity.getTitle());
        revertVersion.setContent(targetVersionEntity.getContent());
        revertVersion.setSummary(targetVersionEntity.getSummary());
        revertVersion.setCategory(targetVersionEntity.getCategory());
        revertVersion.setKeywords(targetVersionEntity.getKeywords());
        revertVersion.setAuthor(targetVersionEntity.getAuthor());
        revertVersion.setDepartment(targetVersionEntity.getDepartment());
        revertVersion.setFileId(targetVersionEntity.getFileId());
        revertVersion.setChangeDescription(commitMessage);
        revertVersion.setCommitMessage(commitMessage);
        revertVersion.setBranch(knowledge.getCurrentBranch() != null ? knowledge.getCurrentBranch() : "main");
        revertVersion.setParentCommitId(findParentCommitId(knowledgeId, revertVersion.getBranch()));
        revertVersion.setCreatedBy(operatorUsername);
        revertVersion.setCreateTime(LocalDateTime.now());
        
        // 生成commit hash
        knowledge.setVersion(newVersion);
        String commitHash = generateCommitHash(knowledge, commitMessage, operatorUsername, revertVersion.getBranch(), revertVersion.getParentCommitId());
        revertVersion.setCommitHash(commitHash);
        
        // 5. 管理员回退：直接发布新版本
        revertVersion.setStatus(Constants.FILE_STATUS_APPROVED);
        revertVersion.setIsPublished(true);
        knowledge.setPublishedVersion(newVersion);
        knowledge.setHasDraft(false);
        knowledge.setStatus(Constants.FILE_STATUS_APPROVED);
        
        // 取消之前的发布标记
        LambdaQueryWrapper<KnowledgeVersion> oldWrapper = new LambdaQueryWrapper<>();
        oldWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        oldWrapper.eq(KnowledgeVersion::getIsPublished, true);
        List<KnowledgeVersion> oldVersions = knowledgeVersionMapper.selectList(oldWrapper);
        for (KnowledgeVersion v : oldVersions) {
            v.setIsPublished(false);
            knowledgeVersionMapper.updateById(v);
        }
        
        knowledgeVersionMapper.insert(revertVersion);
        
        // 6. 更新knowledge表内容
        knowledge.setTitle(targetVersionEntity.getTitle());
        knowledge.setContent(targetVersionEntity.getContent());
        knowledge.setSummary(targetVersionEntity.getSummary());
        knowledge.setCategory(targetVersionEntity.getCategory());
        knowledge.setKeywords(targetVersionEntity.getKeywords());
        knowledge.setAuthor(targetVersionEntity.getAuthor());
        knowledge.setDepartment(targetVersionEntity.getDepartment());
        knowledge.setFileId(targetVersionEntity.getFileId());
        knowledge.setVersion(newVersion);
        knowledge.setCurrentCommitHash(commitHash);
        knowledge.setUpdateTime(LocalDateTime.now());
        knowledge.setUpdateBy(operatorUsername);
        
        knowledgeMapper.updateById(knowledge);
        
        log.info("版本回退成功 - 知识ID: {}, 回退到版本 {} 的内容, 新版本号: {}, 操作者(管理员): {}", 
                knowledgeId, targetVersion, newVersion, operatorUsername);
        
        // 7. 更新搜索索引
        try {
            KnowledgeDTO dto = new KnowledgeDTO();
            BeanUtils.copyProperties(knowledge, dto);
            searchService.updateIndex(dto);
        } catch (Exception e) {
            log.warn("回退后更新索引失败: knowledgeId={}", knowledgeId, e);
        }
        
        KnowledgeDTO result = new KnowledgeDTO();
        BeanUtils.copyProperties(knowledge, result);
        return result;
    }
}

