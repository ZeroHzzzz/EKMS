package com.knowledge.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.api.dto.DepartmentDTO;
import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.KnowledgeQueryDTO;
import com.knowledge.api.dto.KnowledgeVersionDTO;
import com.knowledge.api.dto.StatisticsDTO;
import com.knowledge.api.dto.UserDTO;
import com.knowledge.api.service.AuditService;
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
    
    @Resource
    private com.knowledge.api.service.PermissionService permissionService;
    
    @DubboReference(check = false, timeout = 10000)
    private DepartmentService departmentService;
    
    @DubboReference(check = false, timeout = 10000)
    private AuditService auditService;

    @DubboReference(check = false, timeout = 10000)
    private com.knowledge.api.service.CommentService commentService;

    @DubboReference(check = false, timeout = 10000)
    private com.knowledge.api.service.KnowledgeRelationService relationService;

    @Override
    @Transactional
    public KnowledgeDTO createKnowledge(KnowledgeDTO knowledgeDTO) {
        Knowledge knowledge = new Knowledge();
        BeanUtils.copyProperties(knowledgeDTO, knowledge);
        
        // Check if creator is admin
        boolean isAdmin = false;
        try {
            if (knowledgeDTO.getCreateBy() != null) {
                UserDTO user = userService.getUserByUsername(knowledgeDTO.getCreateBy());
                if (user != null && Constants.ROLE_ADMIN.equals(user.getRole())) {
                    isAdmin = true;
                }
            }
        } catch (Exception e) {
            log.warn("Check user role failed: {}", e.getMessage());
        }

        // Determine status: Pending if file exists (Require Audit for file uploads); else Approved (Text only)
        String status = Constants.FILE_STATUS_APPROVED;
        if (knowledgeDTO.getFileId() != null) {
            status = Constants.FILE_STATUS_PENDING;
        }
        
        knowledge.setStatus(status);
        knowledge.setClickCount(0L);
        knowledge.setCollectCount(0L);
        knowledge.setVersion(1L);
        knowledge.setHasDraft(Constants.FILE_STATUS_PENDING.equals(status)); // If pending, it is effectively a draft
        knowledge.setCurrentBranch("main");
        knowledge.setCreateTime(LocalDateTime.now());
        
        // Set published version ONLY if Approved
        if (Constants.FILE_STATUS_APPROVED.equals(status)) {
            knowledge.setPublishedVersion(1L);
        } else {
            // If Pending, published version is 0 or null? Let's say 0 to indicate none.
            knowledge.setPublishedVersion(0L); 
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
        initialVersion.setIsPublished(Constants.FILE_STATUS_APPROVED.equals(status));
        knowledgeVersionMapper.insert(initialVersion);
        
        // 提取并保存文档全文内容
        if (knowledge.getFileId() != null) {
            String fullText = extractTextFromFile(knowledge.getFileId());
            if (fullText != null) {
                knowledge.setContentText(fullText);
                knowledgeMapper.updateById(knowledge);
            }
        }
        
        // 如果状态是已发布，则添加到搜索引擎索引
        if (Constants.FILE_STATUS_APPROVED.equals(knowledge.getStatus())) {
            try {
                KnowledgeDTO dto = new KnowledgeDTO();
                BeanUtils.copyProperties(knowledge, dto);
                searchService.indexKnowledge(dto);
            } catch (Exception e) {
                log.warn("创建知识后添加索引失败: knowledgeId={}", knowledge.getId(), e);
            }
        }
        
        KnowledgeDTO result = new KnowledgeDTO();
        BeanUtils.copyProperties(knowledge, result);
        log.info("Knowledge created: id={}", knowledge.getId());
        return result;
    }

    @Override
    @Transactional
    public KnowledgeDTO updateKnowledge(KnowledgeDTO knowledgeDTO) {
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeDTO.getId());
        if (knowledge == null) {
            throw new RuntimeException("知识不存在");
        }
        
        // 权限检查：确保只有EDIT权限的用户可以更新
        if (knowledgeDTO.getUpdateBy() != null) {
            UserDTO user = userService.getUserByUsername(knowledgeDTO.getUpdateBy());
            if (user != null) {
                boolean canEdit = permissionService.hasPermission(knowledge.getId(), user.getId(), "EDIT");
                if (!canEdit) {
                    throw new RuntimeException("无权编辑该文档");
                }
            }
        }
        
        // --- Redesign: Always create Draft Version (Decouple Edit from Publish) ---
        log.info("Update logic: Creating Draft Version for knowledgeId: {}", knowledge.getId());
        
        // 1. Update main table metadata (status flags only)
        knowledge.setHasDraft(true);
        knowledge.setUpdateTime(LocalDateTime.now());
        knowledge.setUpdateBy(knowledgeDTO.getUpdateBy());
        // Do NOT update title/content/fileId here. The public version remains unchanged.
        
        if (knowledgeDTO.getIsPrivate() != null) {
             knowledge.setIsPrivate(knowledgeDTO.getIsPrivate());
        }
        
        knowledgeMapper.updateById(knowledge);
        
        // 2. Create Draft Version
        // The version number increments
        Long nextVersion = (knowledge.getVersion() == null ? 0 : knowledge.getVersion()) + 1;
        
        // Use helper to save the DRAFT content
        // Note: we pass knowledgeDTO which contains the NEW content, but 'knowledge' has the OLD content.
        // The saveVersionHistoryWithStatus method must use the DTO's content for the version.
        saveVersionHistoryWithStatus(knowledge, knowledgeDTO, nextVersion, Constants.FILE_STATUS_PENDING, false, true);
        
        KnowledgeDTO result = new KnowledgeDTO();
        BeanUtils.copyProperties(knowledge, result);
        // Important: Return the DRAFT content in the result so frontend sees the update
        if (knowledgeDTO.getTitle() != null) result.setTitle(knowledgeDTO.getTitle());
        if (knowledgeDTO.getContent() != null) result.setContent(knowledgeDTO.getContent());
        
        return result;
    }
    
    @Override
    @Transactional
    public void archiveVersion(Long knowledgeId, String versionName, String operatorUsername) {
        log.info("Archiving version for knowledgeId: {}, name: {}", knowledgeId, versionName);
        
        // 1. Find the latest version (which is the current draft state)
        LambdaQueryWrapper<KnowledgeVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        wrapper.orderByDesc(KnowledgeVersion::getVersion);
        wrapper.last("LIMIT 1");
        KnowledgeVersion latestVersion = knowledgeVersionMapper.selectOne(wrapper);
        
        if (latestVersion == null) {
            log.warn("No version found to archive for knowledgeId: {}", knowledgeId);
            return;
        }
        
        // 2. Update the version's commit message / description to the archived name
        if (versionName != null && !versionName.isEmpty()) {
            latestVersion.setCommitMessage(versionName);
            latestVersion.setChangeDescription(versionName);
            knowledgeVersionMapper.updateById(latestVersion);
        }
        
        // 3. Publish this version (This updates Knowledge table and Indexes it)
        publishVersion(knowledgeId, latestVersion.getVersion());
        log.info("Archived (Published) version {} successfully.", latestVersion.getVersion());
    }
    
    /**
     * 保存版本历史（带状态）
     */
    private void saveVersionHistoryWithStatus(Knowledge knowledge, KnowledgeDTO dto, Long newVersion, 
                                              String versionStatus, boolean isPublished, boolean isUpdate) {
        log.info("saveVersionHistoryWithStatus 开始: knowledgeId={}, newVersion={}, isUpdate={}", 
                knowledge.getId(), newVersion, isUpdate);
        
        KnowledgeVersion version;
        
        if (isUpdate) {
            // 查询要更新的版本
            LambdaQueryWrapper<KnowledgeVersion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(KnowledgeVersion::getKnowledgeId, knowledge.getId());
            wrapper.eq(KnowledgeVersion::getVersion, newVersion);
            version = knowledgeVersionMapper.selectOne(wrapper);
            
            if (version == null) {
                log.warn("期望更新版本 v{} 但未找到，回退到插入模式", newVersion);
                version = new KnowledgeVersion();
                isUpdate = false;
            } else {
                log.info("找到要更新的版本: id={}, version={}", version.getId(), version.getVersion());
            }
        } else {
            version = new KnowledgeVersion();
        }
        
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
        
        // 设置基础版本（用于冲突检测）
        // 只有新创建草稿时才设置 baseVersion，更新现有草稿时保持原有的 baseVersion
        if (!isUpdate) {
            // 记录当前发布版本作为基点
            version.setBaseVersion(knowledge.getPublishedVersion());
            log.info("设置 baseVersion={} (当前发布版本)", knowledge.getPublishedVersion());
        }
        
        if (isUpdate) {
            knowledgeVersionMapper.updateById(version);
            log.info("更新现有待审核版本 - 知识ID: {}, 版本: {}", knowledge.getId(), newVersion);
        } else {
            knowledgeVersionMapper.insert(version);
            log.info("保存版本历史 - 知识ID: {}, 版本: {}, 状态: {}, 是否发布: {}, baseVersion: {}", 
                    knowledge.getId(), newVersion, versionStatus, isPublished, version.getBaseVersion());
        }
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
    
    // Check permission logic should be in controller or a helper, but basic retrieval implies 'exists'.
    // Detailed permission check is in getKnowledgeById with user context if needed, 
    // but interface signature is simple here. Controller should guard.

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
            // 如果查询的是待审核状态，也应该包含那些虽已发布但有待审核草稿的记录
            // 如果查询的是待审核状态，也应该包含那些虽已发布但有待审核草稿的记录
            if (Constants.FILE_STATUS_PENDING.equals(queryDTO.getStatus())) {
                wrapper.and(w -> w.eq(Knowledge::getStatus, Constants.FILE_STATUS_PENDING)
                              .or()
                              .eq(Knowledge::getHasDraft, true));
            } else {
                wrapper.eq(Knowledge::getStatus, queryDTO.getStatus());
            }
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
        
        // 0. [Hard Delete Prep] 收集所有需要清理的文件ID (包括主表和历史版本)
        java.util.Set<Long> fileIdsToCheck = new java.util.HashSet<>();
        if (knowledge.getFileId() != null) {
            fileIdsToCheck.add(knowledge.getFileId());
        }
        
        // 从版本历史中收集文件ID
        LambdaQueryWrapper<KnowledgeVersion> versionQuery = new LambdaQueryWrapper<>();
        versionQuery.select(KnowledgeVersion::getFileId);
        versionQuery.eq(KnowledgeVersion::getKnowledgeId, id);
        versionQuery.isNotNull(KnowledgeVersion::getFileId);
        List<KnowledgeVersion> versions = knowledgeVersionMapper.selectList(versionQuery);
        for (KnowledgeVersion v : versions) {
            if (v.getFileId() != null) {
                fileIdsToCheck.add(v.getFileId());
            }
        }
        log.info("删除知识: id={}, 收集到 {} 个关联文件待检查清理", id, fileIdsToCheck.size());
        
        // 1. 删除所有关联的审核记录
        auditService.deleteByKnowledgeId(id);
        
        // 2. 删除所有版本历史
        LambdaQueryWrapper<KnowledgeVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(KnowledgeVersion::getKnowledgeId, id);
        knowledgeVersionMapper.delete(versionWrapper);
        
        // 3. 删除所有评论
        try {
            commentService.deleteByKnowledgeId(id);
        } catch (Exception e) {
            log.warn("删除关联评论失败", e);
        }
        
        // 4. 删除关联关系
        try {
            relationService.deleteByKnowledgeId(id);
        } catch (Exception e) {
            log.warn("删除关联关系失败", e);
        }

        // 5. 删除ElasticSearch索引
        try {
            searchService.deleteIndex(id);
        } catch (Exception e) {
            log.warn("删除索引失败", e);
        }

        // 6. 删除知识记录
        boolean deleted = knowledgeMapper.deleteById(id) > 0;
        
        // 7. [Hard Delete Exec] 检查并硬删除文件
        if (deleted && !fileIdsToCheck.isEmpty()) {
            for (Long fileId : fileIdsToCheck) {
                try {
                    // 检查 fileId 是否还被其他 knowledge 引用
                    LambdaQueryWrapper<Knowledge> kWrap = new LambdaQueryWrapper<>();
                    kWrap.eq(Knowledge::getFileId, fileId);
                    Long kCount = knowledgeMapper.selectCount(kWrap);
                    
                    if (kCount > 0) {
                        log.info("文件仍在其他 Knowledge 中使用，跳过删除: fileId={}", fileId);
                        continue;
                    }
                    
                    // 检查 fileId 是否还被其他 knowledge_version 引用
                    // 注意：由于我们已经删除了当前 knowledge 的 versions，这里查到的只能是其他 knowledge 的 version
                    LambdaQueryWrapper<KnowledgeVersion> vWrap = new LambdaQueryWrapper<>();
                    vWrap.eq(KnowledgeVersion::getFileId, fileId);
                    Long vCount = knowledgeVersionMapper.selectCount(vWrap);
                    
                    if (vCount > 0) {
                        log.info("文件仍在其他 Version 中使用，跳过删除: fileId={}", fileId);
                        continue;
                    }
                    
                    // 无引用，安全删除
                    fileService.deleteFile(fileId);
                    log.info("硬删除孤儿文件成功: fileId={}", fileId);
                    
                } catch (Exception e) {
                    log.error("硬删除文件失败: fileId={}", fileId, e);
                }
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
                // 只有已发布的才更新索引
                if (Constants.FILE_STATUS_APPROVED.equals(knowledge.getStatus())) {
                    KnowledgeDTO dto = new KnowledgeDTO();
                    BeanUtils.copyProperties(knowledge, dto);
                    searchService.updateIndex(dto);
                }
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
    @Transactional
    public boolean publishVersion(Long knowledgeId, Long version) {
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null) {
            log.error("发布版本失败：知识不存在 knowledgeId={}", knowledgeId);
            return false;
        }
        
        // 查找要发布的版本
        LambdaQueryWrapper<KnowledgeVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        versionWrapper.eq(KnowledgeVersion::getVersion, version);
        KnowledgeVersion targetVersion = knowledgeVersionMapper.selectOne(versionWrapper);
        
        if (targetVersion == null) {
            log.error("发布版本失败：版本不存在 knowledgeId={}, version={}", knowledgeId, version);
            return false;
        }
        
        // 更新version表：标记目标版本为已发布
        targetVersion.setIsPublished(true);
        targetVersion.setStatus(Constants.FILE_STATUS_APPROVED);
        knowledgeVersionMapper.updateById(targetVersion);
        
        // 将之前的发布版本标记为非发布
        LambdaQueryWrapper<KnowledgeVersion> oldVersionWrapper = new LambdaQueryWrapper<>();
        oldVersionWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        oldVersionWrapper.ne(KnowledgeVersion::getVersion, version);
        oldVersionWrapper.eq(KnowledgeVersion::getIsPublished, true);
        List<KnowledgeVersion> oldPublishedVersions = knowledgeVersionMapper.selectList(oldVersionWrapper);
        for (KnowledgeVersion oldVersion : oldPublishedVersions) {
            oldVersion.setIsPublished(false);
            knowledgeVersionMapper.updateById(oldVersion);
        }
        
        // 更新knowledge表：用发布版本的内容更新主表
        knowledge.setTitle(targetVersion.getTitle());
        knowledge.setContent(targetVersion.getContent());
        knowledge.setSummary(targetVersion.getSummary());
        knowledge.setCategory(targetVersion.getCategory());
        knowledge.setKeywords(targetVersion.getKeywords());
        if (targetVersion.getFileId() != null) {
            knowledge.setFileId(targetVersion.getFileId());
            // 重新提取文件内容用于搜索
            String fullText = extractTextFromFile(targetVersion.getFileId());
            if (fullText != null) {
                knowledge.setContentText(fullText);
            }
        }
        knowledge.setStatus(Constants.FILE_STATUS_APPROVED);
        knowledge.setPublishedVersion(version);
        
        // Check for newer drafts to correctly set hasDraft
        LambdaQueryWrapper<KnowledgeVersion> newerWrapper = new LambdaQueryWrapper<>();
        newerWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        newerWrapper.gt(KnowledgeVersion::getVersion, version);
        Long newerCount = knowledgeVersionMapper.selectCount(newerWrapper);
        knowledge.setHasDraft(newerCount > 0);

        knowledge.setUpdateTime(LocalDateTime.now());
        boolean result = knowledgeMapper.updateById(knowledge) > 0;
        
        // 同步更新Elasticsearch索引
        if (result) {
            try {
                KnowledgeDTO dto = new KnowledgeDTO();
                BeanUtils.copyProperties(knowledge, dto);
                searchService.updateIndex(dto);
                log.info("发布指定版本并更新索引成功: knowledgeId={}, publishedVersion={}", knowledgeId, version);
            } catch (Exception e) {
                log.warn("发布版本后更新索引失败: knowledgeId={}", knowledgeId, e);
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
    public List<KnowledgeVersionDTO> getKnowledgeVersionsForUser(Long knowledgeId, String username, boolean isAdmin) {
        List<KnowledgeVersionDTO> allVersions = getKnowledgeVersions(knowledgeId);
        
        // 管理员可以看到所有版本
        if (isAdmin) {
            return allVersions;
        }
        
        // 获取知识信息，用于匹配作者
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        
        // 普通用户过滤逻辑：
        // 1. 已发布的版本（isPublished=true）：所有人可见
        // 2. 待审核的版本（status=PENDING）：只有作者可见
        // 3. 其他状态（如 REJECTED）：只有创建者可见
        return allVersions.stream()
            .filter(v -> {
                // 已发布的版本：所有人可见
                if (Boolean.TRUE.equals(v.getIsPublished())) {
                    return true;
                }
                if (Constants.FILE_STATUS_APPROVED.equals(v.getStatus())) {
                    return true;
                }
                // 待审核/未发布版本：只有作者可见
                // 通过多种方式匹配作者身份
                if (username != null) {
                    // 1. 匹配版本的 createdBy
                    if (username.equals(v.getCreatedBy())) {
                        return true;
                    }
                    // 2. 匹配知识的 createBy（知识创建者）
                    if (knowledge != null && username.equals(knowledge.getCreateBy())) {
                        return true;
                    }
                    // 3. 匹配知识的 updateBy（最近更新者）
                    if (knowledge != null && username.equals(knowledge.getUpdateBy())) {
                        return true;
                    }
                    // 4. 匹配知识的 author 字段
                    if (knowledge != null && username.equals(knowledge.getAuthor())) {
                        return true;
                    }
                }
                return false;
            })
            .collect(Collectors.toList());
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
        
        // Debug Log: check fileId
        log.info("getKnowledgeVersion: id={}, version={}, fileId={}, title={}", 
                 knowledgeId, version, dto.getFileId(), dto.getTitle());
                 
        return dto;
    }
    
    @Override
    public KnowledgeVersionDTO.DiffResult compareVersions(Long knowledgeId, Long version1, Long version2) {
        KnowledgeVersionDTO v1 = getKnowledgeVersion(knowledgeId, version1);
        KnowledgeVersionDTO v2 = getKnowledgeVersion(knowledgeId, version2);
        
        if (v1 == null || v2 == null) {
            throw new RuntimeException("版本不存在");
        }
        
        // 获取实际文档内容（优先使用文件提取的文本）
        String content1 = getVersionTextContent(v1);
        String content2 = getVersionTextContent(v2);
        
        // 使用DiffUtil比较内容
        List<DiffUtil.DiffLine> diffLines = DiffUtil.diff(content1, content2);
        
        // 转换为DTO
        List<KnowledgeVersionDTO.DiffResult.DiffLine> diffLineDTOs = diffLines.stream().map(line -> {
            KnowledgeVersionDTO.DiffResult.DiffLine dto = new KnowledgeVersionDTO.DiffResult.DiffLine();
            dto.setType(line.getType().name());
            dto.setContent(line.getContent());
            dto.setLineNumber1(line.getOriginalLineNumber());
            dto.setLineNumber2(line.getNewLineNumber());
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
    
    /**
     * 获取版本的文本内容（用于对比）
     * 优先级：1. 从文件提取的文本（Office文档实际内容） 2. content字段（用户填写的摘要）
     */
    private String getVersionTextContent(KnowledgeVersionDTO version) {
        // 如果有关联文件，使用 Tika 提取实际内容
        if (version.getFileId() != null) {
            String extractedText = extractTextFromFile(version.getFileId());
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                log.debug("使用文件提取内容进行版本对比: fileId={}, length={}", 
                        version.getFileId(), extractedText.length());
                return extractedText;
            }
        }
        // 回退到 content 字段（用于没有文件的纯文本知识）
        return version.getContent() != null ? version.getContent() : "";
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
            if (Constants.FILE_STATUS_APPROVED.equals(knowledge.getStatus())) {
                KnowledgeDTO dto = new KnowledgeDTO();
                BeanUtils.copyProperties(knowledge, dto);
                searchService.updateIndex(dto);
            }
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
    public KnowledgeDTO getKnowledgeByFileId(Long fileId) {
        if (fileId == null) {
            return null;
        }
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Knowledge::getFileId, fileId);
        wrapper.last("LIMIT 1");
        Knowledge knowledge = knowledgeMapper.selectOne(wrapper);
        
        // 如果在主表中找不到，尝试在历史版本表中查找
        if (knowledge == null) {
            LambdaQueryWrapper<KnowledgeVersion> versionWrapper = new LambdaQueryWrapper<>();
            versionWrapper.eq(KnowledgeVersion::getFileId, fileId);
            versionWrapper.last("LIMIT 1");
            KnowledgeVersion version = knowledgeVersionMapper.selectOne(versionWrapper);
            
            if (version != null) {
                // 如果在历史版本中找到了，获取对应的主知识记录
                knowledge = knowledgeMapper.selectById(version.getKnowledgeId());
            }
        }
        
        if (knowledge == null) {
            return null;
        }
        KnowledgeDTO dto = new KnowledgeDTO();
        BeanUtils.copyProperties(knowledge, dto);
        return dto;
    }
    
    @Override
    @Transactional
    public KnowledgeDTO createVersionFromFileEdit(Long knowledgeId, Long newFileId, String operatorUsername, Long operatorId, String changeDescription) {
        // 1. 获取当前知识
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null) {
            throw new RuntimeException("知识不存在");
        }
        
        // 2. 检查操作者是否是管理员
        boolean isAdmin = false;
        Long userId = operatorId;
        try {
            UserDTO user = null;
            if (operatorId != null) {
                 // 优先使用 ID 查找
                 user = userService.getUserById(operatorId);
            }
            
            if (user == null && operatorUsername != null) {
                 // 回退到使用用户名查找
                user = userService.getUserByUsername(operatorUsername);
            }
            
            if (user != null) {
                isAdmin = Constants.ROLE_ADMIN.equals(user.getRole());
                userId = user.getId();
            }
        } catch (Exception e) {
            log.warn("检查用户角色失败: {}", e.getMessage());
        }
        
        // 5. 保存版本历史
        // 检查最新版本状态，如果是待审核且未发布，则更新该版本
        LambdaQueryWrapper<KnowledgeVersion> latestWrapper = new LambdaQueryWrapper<>();
        latestWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        latestWrapper.orderByDesc(KnowledgeVersion::getVersion);
        latestWrapper.last("LIMIT 1");
        KnowledgeVersion latestVersion = knowledgeVersionMapper.selectOne(latestWrapper);
        
        // 5. 确定新版本的状态
        // 修正逻辑：在线编辑生成的版本直接为“已发布”(APPROVED)，不需要审核。
        String versionStatus = Constants.FILE_STATUS_APPROVED;
        boolean isPublished = true;
        
        knowledge.setHasDraft(false); // 直接发布，无草稿
        // 如果从未发布过，且原来的状态为空，则设为 APPROVED
        if (knowledge.getStatus() == null) {
            knowledge.setStatus(Constants.FILE_STATUS_APPROVED);
        }

        log.info("用户编辑文件，直接创建已发布版本 - 知识ID: {}", knowledge.getId());

        // Fix declarations
        Long newVersion;
        KnowledgeVersion version;
        boolean isUpdateExisting = false;

        Long maxVersion = (latestVersion != null) ? latestVersion.getVersion() : (knowledge.getVersion() != null ? knowledge.getVersion() : 0L);
        newVersion = maxVersion + 1;
        version = new KnowledgeVersion();
        
        // Ensure main entity status is APPROVED
        knowledge.setStatus(Constants.FILE_STATUS_APPROVED);
        knowledge.setHasDraft(false);
        
        log.info("Creating NEW Published Version v{} for save", newVersion);

        // 设置版本属性
        version.setKnowledgeId(knowledge.getId());
        version.setVersion(newVersion);
        version.setCreateTime(LocalDateTime.now());
        
        version.setTitle(knowledge.getTitle());
        version.setContent(knowledge.getContent());
        version.setSummary(knowledge.getSummary());
        version.setCategory(knowledge.getCategory());
        version.setKeywords(knowledge.getKeywords());
        version.setAuthor(knowledge.getAuthor());
        version.setDepartment(knowledge.getDepartment());
        version.setFileId(newFileId);  // 使用新文件ID
        
        String commitMessage = (changeDescription != null && !changeDescription.trim().isEmpty()) 
            ? changeDescription : "通过OnlyOffice编辑更新";
        version.setChangeDescription(commitMessage);
        version.setCommitMessage(commitMessage);
        
        version.setCreatedBy(operatorUsername);
        
        String branch = "main"; // 直接归入主分支
        version.setBranch(branch);
        
        Long parentCommitId = findParentCommitId(knowledge.getId(), branch);
        version.setParentCommitId(parentCommitId);
        
        // 临时设置版本号用于生成hash
        knowledge.setVersion(newVersion);
        String commitHash = generateCommitHash(knowledge, commitMessage, operatorUsername, branch, version.getParentCommitId());
        version.setCommitHash(commitHash);
        
        // 设置版本状态
        version.setStatus(versionStatus);
        version.setIsPublished(isPublished);
        
        knowledgeVersionMapper.insert(version);
        log.info("文件编辑创建新发布版本 - 知识ID: {}, 版本: {}, 新文件ID: {}, 状态: {}, 操作者: {}", 
                knowledgeId, newVersion, newFileId, versionStatus, operatorUsername);
        
        // 6. 更新knowledge表
        // 始终更新主表内容到最新版本
        knowledge.setFileId(newFileId);  // 更新到新文件
        knowledge.setVersion(newVersion);
        knowledge.setPublishedVersion(newVersion); // 设置为当前发布版本
        knowledge.setCurrentCommitHash(commitHash);
        
        // 重新提取文件内容用于搜索
        if (newFileId != null) {
            String fullText = extractTextFromFile(newFileId);
            if (fullText != null) {
                knowledge.setContentText(fullText);
            }
        }
        knowledge.setHasDraft(false);
        
        knowledge.setUpdateTime(LocalDateTime.now());
        knowledge.setUpdateBy(operatorUsername);
        
        // 取消之前版本的发布标记
        if (isPublished) {
            LambdaQueryWrapper<KnowledgeVersion> oldWrapper = new LambdaQueryWrapper<>();
            oldWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
            oldWrapper.ne(KnowledgeVersion::getVersion, newVersion);
            oldWrapper.eq(KnowledgeVersion::getIsPublished, true);
            List<KnowledgeVersion> oldVersions = knowledgeVersionMapper.selectList(oldWrapper);
            for (KnowledgeVersion v : oldVersions) {
                v.setIsPublished(false);
                knowledgeVersionMapper.updateById(v);
            }
        }
        
        knowledgeMapper.updateById(knowledge);
        
        // 7. 不需要审核记录 (Direct Publish)
        
        // 8. 更新搜索索引
        try {
            KnowledgeDTO dto = new KnowledgeDTO();
            BeanUtils.copyProperties(knowledge, dto);
            searchService.updateIndex(dto);
        } catch (Exception e) {
            log.warn("更新索引失败: knowledgeId={}", knowledgeId, e);
        }
        
        KnowledgeDTO result = new KnowledgeDTO();
        BeanUtils.copyProperties(knowledge, result);
        return result;
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
        
        // 记录当前版本号，用于日志
        Long currentVersion = knowledge.getVersion();
        
        // 4. 删除目标版本之后的所有版本历史记录（包括当前版本）
        // 关键修复：使用 gt（大于）而不是 gte（大于等于），因为我们要保留目标版本
        LambdaQueryWrapper<KnowledgeVersion> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        deleteWrapper.gt(KnowledgeVersion::getVersion, targetVersion);
        int deletedCount = knowledgeVersionMapper.delete(deleteWrapper);
        log.info("版本回退：删除版本范围 v{} 到 v{}，共删除 {} 个版本", 
                targetVersion + 1, currentVersion, deletedCount);
        
        // 5. 删除目标版本之后的审核记录
        try {
            auditService.deleteByKnowledgeIdAndVersionGt(knowledgeId, targetVersion);
            log.info("已删除版本 {} 之后的审核记录", targetVersion);
        } catch (Exception e) {
            log.warn("删除后续审核记录失败: {}", e.getMessage());
        }
        
        // 6. 在标记目标版本为已发布之前，先取消所有其他版本的发布标记
        // 这样确保只有目标版本的 isPublished = true
        LambdaQueryWrapper<KnowledgeVersion> unpublishWrapper = new LambdaQueryWrapper<>();
        unpublishWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        unpublishWrapper.eq(KnowledgeVersion::getIsPublished, true);
        List<KnowledgeVersion> publishedVersions = knowledgeVersionMapper.selectList(unpublishWrapper);
        for (KnowledgeVersion v : publishedVersions) {
            v.setIsPublished(false);
            knowledgeVersionMapper.updateById(v);
        }
        log.info("已取消 {} 个版本的发布标记", publishedVersions.size());
        
        // 7. 将目标版本标记为已发布
        targetVersionEntity.setIsPublished(true);
        targetVersionEntity.setStatus(Constants.FILE_STATUS_APPROVED);
        knowledgeVersionMapper.updateById(targetVersionEntity);
        
        // 8. 更新knowledge表内容为目标版本的内容
        knowledge.setTitle(targetVersionEntity.getTitle());
        knowledge.setContent(targetVersionEntity.getContent());
        knowledge.setSummary(targetVersionEntity.getSummary());
        knowledge.setCategory(targetVersionEntity.getCategory());
        knowledge.setKeywords(targetVersionEntity.getKeywords());
        knowledge.setAuthor(targetVersionEntity.getAuthor());
        knowledge.setDepartment(targetVersionEntity.getDepartment());
        knowledge.setFileId(targetVersionEntity.getFileId());
        knowledge.setVersion(targetVersion);  // 版本号回退到目标版本
        knowledge.setPublishedVersion(targetVersion);
        knowledge.setHasDraft(false);
        knowledge.setStatus(Constants.FILE_STATUS_APPROVED);
        knowledge.setCurrentCommitHash(targetVersionEntity.getCommitHash());
        knowledge.setUpdateTime(LocalDateTime.now());
        knowledge.setUpdateBy(operatorUsername);
        
        // 重新提取文件内容用于搜索
        if (targetVersionEntity.getFileId() != null) {
            String fullText = extractTextFromFile(targetVersionEntity.getFileId());
            if (fullText != null) {
                knowledge.setContentText(fullText);
            }
        }
        
        knowledgeMapper.updateById(knowledge);
        
        log.info("版本回退成功（删除后续版本） - 知识ID: {}, 回退到版本: {}, 删除了 {} 个后续版本, 操作者(管理员): {}", 
                knowledgeId, targetVersion, deletedCount, operatorUsername);
        
        // 8. 更新搜索索引
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
    
    @Override
    public com.knowledge.api.dto.MergeStatusDTO checkMergeStatus(Long knowledgeId, Long draftVersion) {
        com.knowledge.api.dto.MergeStatusDTO status = new com.knowledge.api.dto.MergeStatusDTO();
        status.setIncomingVersion(draftVersion);
        
        // 1. 获取草稿版本 (Theirs)
        LambdaQueryWrapper<KnowledgeVersion> draftWrapper = new LambdaQueryWrapper<>();
        draftWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        draftWrapper.eq(KnowledgeVersion::getVersion, draftVersion);
        KnowledgeVersion draftVer = knowledgeVersionMapper.selectOne(draftWrapper);
        
        if (draftVer == null) {
            throw new RuntimeException("Draft version not found: " + draftVersion);
        }
        
        // 2. 获取当前发布版本 (Ours / Target)
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null) {
            throw new RuntimeException("Knowledge not found: " + knowledgeId);
        }
        
        Long currentPublishedVersion = knowledge.getPublishedVersion();
        status.setTargetVersion(currentPublishedVersion);
        
        // 3. 使用 baseVersion 检测冲突
        Long draftBaseVersion = draftVer.getBaseVersion();
        status.setBaseVersion(draftBaseVersion);
        
        log.info("冲突检测: 草稿v{} 基于v{}, 当前发布版本v{}", 
                draftVersion, draftBaseVersion, currentPublishedVersion);
        
        // 如果没有发布版本（初始发布场景）-> 无冲突，可以直接发布
        if (currentPublishedVersion == null) {
            status.setHasConflict(false);
            status.setCanAutoMerge(true);
            status.setConflictDetails("初始发布，无冲突");
            status.setMergedContent(draftVer.getContent());
            return status;
        }
        
        // 4. 检查是否为 Fast-Forward（草稿基于当前发布版本）
        boolean isBasedOnCurrent = (draftBaseVersion != null && draftBaseVersion.equals(currentPublishedVersion))
                || (draftBaseVersion == null && currentPublishedVersion.equals(1L)); // 兼容旧数据
        
        if (isBasedOnCurrent) {
            // Fast-Forward: 草稿基于当前版本，无冲突
            status.setHasConflict(false);
            status.setCanAutoMerge(true);
            status.setConflictDetails("Fast-Forward: 草稿基于当前发布版本，可直接发布");
            status.setMergedContent(draftVer.getContent());
            log.info("Fast-Forward 合并: 草稿v{} 可直接发布", draftVersion);
            return status;
        }
        
        // 5. 草稿基于旧版本 -> 需要进行冲突检测
        String conflictMessage = String.format(
            "草稿基于版本 v%d 创建，但当前发布版本已更新到 v%d。" +
            "如果发布此草稿，可能会覆盖其他用户的修改。",
            draftBaseVersion != null ? draftBaseVersion : 0,
            currentPublishedVersion
        );
        
        // 6. 获取内容进行三方合并
        String targetContent = "";
        String baseContent = "";
        
        // 获取当前发布版本内容
        LambdaQueryWrapper<KnowledgeVersion> targetWrapper = new LambdaQueryWrapper<>();
        targetWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        targetWrapper.eq(KnowledgeVersion::getVersion, currentPublishedVersion);
        KnowledgeVersion targetVer = knowledgeVersionMapper.selectOne(targetWrapper);
        if (targetVer != null) {
            targetContent = targetVer.getContent() != null ? targetVer.getContent() : "";
        }
        
        // 获取基础版本内容
        if (draftBaseVersion != null) {
            LambdaQueryWrapper<KnowledgeVersion> baseWrapper = new LambdaQueryWrapper<>();
            baseWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
            baseWrapper.eq(KnowledgeVersion::getVersion, draftBaseVersion);
            KnowledgeVersion baseVer = knowledgeVersionMapper.selectOne(baseWrapper);
            if (baseVer != null) {
                baseContent = baseVer.getContent() != null ? baseVer.getContent() : "";
            }
        }
        
        String incomingContent = draftVer.getContent() != null ? draftVer.getContent() : "";
        
        // 7. 执行三方合并
        DiffUtil.MergeResult mergeResult = DiffUtil.merge(baseContent, targetContent, incomingContent);
        String merged = mergeResult.getMergedContent();
        status.setMergedContent(merged);
        
        if (mergeResult.isHasConflict()) {
            status.setHasConflict(true);
            status.setCanAutoMerge(false);
            status.setConflictDetails(conflictMessage + " 检测到内容冲突，需要手动解决。");
            log.info("检测到冲突: 草稿v{} 与当前版本v{} 有内容冲突", draftVersion, currentPublishedVersion);
        } else {
            // 虽然基于旧版本，但内容可以自动合并
            status.setHasConflict(false);
            status.setCanAutoMerge(true);
            status.setConflictDetails(conflictMessage + " 内容可以自动合并。");
            log.info("自动合并成功: 草稿v{} 可以与当前版本v{} 自动合并", draftVersion, currentPublishedVersion);
        }
        
        return status;
    }


    @Override
    @Transactional
    public KnowledgeDTO mergeAndPublish(Long knowledgeId, Long draftVersion, String resolvedContent) {
        // 1. Must check merge status first or rely on provided resolvedContent
        com.knowledge.api.dto.MergeStatusDTO status = checkMergeStatus(knowledgeId, draftVersion);
        
        String finalContent;
        if (resolvedContent != null && !resolvedContent.isEmpty()) {
            finalContent = resolvedContent;
        } else {
            if (status.isHasConflict()) {
                throw new RuntimeException("Merge conflict detected. Please resolve conflicts manually.");
            }
            finalContent = status.getMergedContent();
        }
        
        // 2. 创建合并后的新版本 (Merge Commit) OR 直接发布 (Inital Publish)
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        
        // Retrieve draft version first as it's needed in both paths
        LambdaQueryWrapper<KnowledgeVersion> draftWrapper = new LambdaQueryWrapper<>();
        draftWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        draftWrapper.eq(KnowledgeVersion::getVersion, draftVersion);
        KnowledgeVersion draftVer = knowledgeVersionMapper.selectOne(draftWrapper);

        // 检查是否是初始版本发布（之前没有发布过）
        boolean isInitialPublish = (knowledge.getPublishedVersion() == null);
        
        if (isInitialPublish) {
             log.info("初始版本发布优化：直接将草稿晋升为发布版本 (v{})", draftVersion);
             
             if (draftVer == null) {
                 throw new RuntimeException("找不到待发布的草稿版本");
             }
             
             // 不创建新版本，直接更新草稿状态
             draftVer.setStatus(Constants.FILE_STATUS_APPROVED);
             draftVer.setIsPublished(true);
             draftVer.setBranch("main"); // 初始版本归入主要分支
             draftVer.setCommitMessage("初始版本发布");
             // 保持原 authors 等信息
             
             knowledgeVersionMapper.updateById(draftVer);
             
             // Update Knowledge Main
             knowledge.setVersion(draftVersion); // v1
             knowledge.setPublishedVersion(draftVersion);
             knowledge.setContent(draftVer.getContent());
             knowledge.setTitle(draftVer.getTitle());
             knowledge.setCategory(draftVer.getCategory());
             knowledge.setSummary(draftVer.getSummary());
             knowledge.setKeywords(draftVer.getKeywords());
             knowledge.setFileId(draftVer.getFileId());
             if (draftVer.getFileId() != null) {
                String fullText = extractTextFromFile(draftVer.getFileId());
                if (fullText != null) {
                    knowledge.setContentText(fullText);
                }
             }
             knowledge.setCurrentBranch("main");
             
        } else {
             // 3. Clear previous published flags
             LambdaQueryWrapper<KnowledgeVersion> oldPublishedWrapper = new LambdaQueryWrapper<>();
             oldPublishedWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
             oldPublishedWrapper.eq(KnowledgeVersion::getIsPublished, true);
             List<KnowledgeVersion> oldPublished = knowledgeVersionMapper.selectList(oldPublishedWrapper);
             for (KnowledgeVersion old : oldPublished) {
                 old.setIsPublished(false);
                 knowledgeVersionMapper.updateById(old);
             }

             // 非初始版本，执行标准的3-way merge并创建新版本
             Long newVersion = (knowledgeVersionMapper.selectMaxVersion(knowledgeId)) + 1;
             
             KnowledgeVersion mergeVer = new KnowledgeVersion();
             mergeVer.setKnowledgeId(knowledgeId);
             mergeVer.setVersion(newVersion);
             mergeVer.setContent(finalContent);
             
             if (draftVer != null) {
                 mergeVer.setTitle(draftVer.getTitle());
                 mergeVer.setCategory(draftVer.getCategory());
                 mergeVer.setSummary(draftVer.getSummary());
                 mergeVer.setAuthor(draftVer.getAuthor()); 
                 mergeVer.setCreatedBy(draftVer.getCreatedBy()); 
                 mergeVer.setFileId(draftVer.getFileId());
             } else {
                 mergeVer.setTitle(knowledge.getTitle());
                 mergeVer.setCreatedBy(knowledge.getUpdateBy());
             }
             
             if (draftVer != null) {
                 mergeVer.setMergeFromVersion(draftVer.getVersion());
             }
             
             // Improve Commit Message
             String userMsg = draftVer != null ? draftVer.getChangeDescription() : "";
             String commitMsg = "Merge: " + (userMsg != null && !userMsg.isEmpty() ? userMsg : "Update content");
             
             mergeVer.setChangeDescription(commitMsg);
             mergeVer.setCommitMessage(commitMsg);
             mergeVer.setBranch("main");
             mergeVer.setStatus(Constants.FILE_STATUS_APPROVED); // Published
             mergeVer.setIsPublished(true);
             mergeVer.setCreateTime(LocalDateTime.now());
             
             // Generate Hash
             Long parentCommitId = findParentCommitId(knowledge.getId(), "main");
             mergeVer.setParentCommitId(parentCommitId);
             
             Knowledge tempK = new Knowledge();
             BeanUtils.copyProperties(knowledge, tempK);
             tempK.setVersion(newVersion);
             String hash = generateCommitHash(tempK, commitMsg, mergeVer.getCreatedBy(), "main", parentCommitId);
             mergeVer.setCommitHash(hash);
             
             knowledgeVersionMapper.insert(mergeVer);
             
             // Update Knowledge Main
             knowledge.setVersion(newVersion);
             knowledge.setPublishedVersion(newVersion);
             knowledge.setContent(finalContent);
             // inherit other fields if changed in draft
             if (draftVer != null) {
                knowledge.setTitle(draftVer.getTitle());
                knowledge.setCategory(draftVer.getCategory());
                knowledge.setSummary(draftVer.getSummary());
                knowledge.setKeywords(draftVer.getKeywords());
                knowledge.setFileId(draftVer.getFileId());
                 if (draftVer.getFileId() != null) {
                    String fullText = extractTextFromFile(draftVer.getFileId());
                    if (fullText != null) {
                        knowledge.setContentText(fullText);
                    }
                 }
             }
             
             // Cleanup: Mark old draft as Merged/Approved
             if (draftVer != null) {
                  draftVer.setStatus(Constants.FILE_STATUS_APPROVED); 
                  knowledgeVersionMapper.updateById(draftVer);
             }
        }
        // inherit other fields if changed in draft
        if (draftVer != null) {
            knowledge.setTitle(draftVer.getTitle());
            knowledge.setCategory(draftVer.getCategory());
            knowledge.setSummary(draftVer.getSummary());
            knowledge.setKeywords(draftVer.getKeywords());
            knowledge.setFileId(draftVer.getFileId());
             if (draftVer.getFileId() != null) {
                String fullText = extractTextFromFile(draftVer.getFileId());
                if (fullText != null) {
                    knowledge.setContentText(fullText);
                }
             }
        }
        
        knowledge.setStatus(Constants.FILE_STATUS_APPROVED);
        knowledge.setHasDraft(false); 
        knowledge.setUpdateTime(LocalDateTime.now());
        knowledgeMapper.updateById(knowledge);
        
        // 4. Update Index
        try {
            KnowledgeDTO dto = new KnowledgeDTO();
            BeanUtils.copyProperties(knowledge, dto);
            searchService.updateIndex(dto);
        } catch (Exception e) {
            log.warn("Index update failed after merge", e);
        }
        
        // 5. Cleanup: 
        if (draftVer != null) {
             draftVer.setStatus(Constants.FILE_STATUS_APPROVED); // Mark draft as Approved/Merged
             knowledgeVersionMapper.updateById(draftVer);
        }

        KnowledgeDTO result = new KnowledgeDTO();
        BeanUtils.copyProperties(knowledge, result);
        return result;
    }

    @Override
    @Transactional
    public boolean rejectVersion(Long knowledgeId, Long version) {
        // 查找指定版本
        LambdaQueryWrapper<KnowledgeVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgeVersion::getVersion, version);
        KnowledgeVersion targetVersion = knowledgeVersionMapper.selectOne(wrapper);
        
        if (targetVersion == null) {
            log.warn("驳回版本失败：版本不存在 knowledgeId={}, version={}", knowledgeId, version);
            return false;
        }
        
        // 更新版本状态为已驳回
        targetVersion.setStatus(Constants.FILE_STATUS_REJECTED);
        targetVersion.setIsPublished(false);
        knowledgeVersionMapper.updateById(targetVersion);
        
        log.info("版本已驳回: knowledgeId={}, version={}", knowledgeId, version);
        return true;
    }
    
    @Override
    @Transactional
    public boolean updateKnowledgeStatus(Long knowledgeId, String status, boolean hasDraft) {
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null) {
            log.warn("更新状态失败：知识不存在 knowledgeId={}", knowledgeId);
            return false;
        }
        
        knowledge.setStatus(status);
        knowledge.setHasDraft(hasDraft);
        knowledge.setUpdateTime(LocalDateTime.now());
        knowledgeMapper.updateById(knowledge);
        
        log.info("知识状态已更新: knowledgeId={}, status={}, hasDraft={}", knowledgeId, status, hasDraft);
        return true;
    }
    
    @Override
    @Transactional
    public void updateVersionCommitMessage(Long knowledgeId, Long version, String commitMessage) {
        LambdaQueryWrapper<KnowledgeVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgeVersion::getVersion, version);
        wrapper.last("LIMIT 1");
        KnowledgeVersion versionEntity = knowledgeVersionMapper.selectOne(wrapper);
        
        if (versionEntity != null) {
            versionEntity.setCommitMessage(commitMessage);
            knowledgeVersionMapper.updateById(versionEntity);
            log.info("版本提交信息已更新: knowledgeId={}, version={}, message={}", knowledgeId, version, commitMessage);
        } else {
            log.warn("更新版本提交信息失败：版本不存在 knowledgeId={}, version={}", knowledgeId, version);
        }
    }
    
    @Override
    public com.knowledge.api.dto.MergePreviewDTO getMergePreview(Long knowledgeId, Long baseVersion, Long currentVersion, Long draftVersion) {
        log.info("获取合并预览: knowledgeId={}, base={}, current={}, draft={}", 
                knowledgeId, baseVersion, currentVersion, draftVersion);
        
        // 获取三个版本的内容
        String baseContent = "";
        String currentContent = "";
        String draftContent = "";
        String draftAuthor = "";
        String currentAuthor = "";
        
        // 获取基础版本
        if (baseVersion != null) {
            LambdaQueryWrapper<KnowledgeVersion> baseWrapper = new LambdaQueryWrapper<>();
            baseWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
            baseWrapper.eq(KnowledgeVersion::getVersion, baseVersion);
            KnowledgeVersion baseVer = knowledgeVersionMapper.selectOne(baseWrapper);
            if (baseVer != null) {
                baseContent = baseVer.getContent() != null ? baseVer.getContent() : "";
            }
        }
        
        // 获取当前发布版本
        if (currentVersion != null) {
            LambdaQueryWrapper<KnowledgeVersion> currentWrapper = new LambdaQueryWrapper<>();
            currentWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
            currentWrapper.eq(KnowledgeVersion::getVersion, currentVersion);
            KnowledgeVersion currentVer = knowledgeVersionMapper.selectOne(currentWrapper);
            if (currentVer != null) {
                currentContent = currentVer.getContent() != null ? currentVer.getContent() : "";
                currentAuthor = currentVer.getCreatedBy();
            }
        }
        
        // 获取草稿版本
        LambdaQueryWrapper<KnowledgeVersion> draftWrapper = new LambdaQueryWrapper<>();
        draftWrapper.eq(KnowledgeVersion::getKnowledgeId, knowledgeId);
        draftWrapper.eq(KnowledgeVersion::getVersion, draftVersion);
        KnowledgeVersion draftVer = knowledgeVersionMapper.selectOne(draftWrapper);
        if (draftVer != null) {
            draftContent = draftVer.getContent() != null ? draftVer.getContent() : "";
            draftAuthor = draftVer.getCreatedBy();
        }
        
        // 构建合并预览
        com.knowledge.api.dto.MergePreviewDTO preview = new com.knowledge.api.dto.MergePreviewDTO();
        preview.setKnowledgeId(knowledgeId);
        preview.setBaseVersion(baseVersion);
        preview.setCurrentVersion(currentVersion);
        preview.setDraftVersion(draftVersion);
        preview.setDraftAuthor(draftAuthor);
        preview.setCurrentVersionAuthor(currentAuthor);
        
        // 使用 DiffUtil 生成块级差异
        java.util.List<com.knowledge.api.dto.MergePreviewDTO.MergeBlock> blocks = new java.util.ArrayList<>();
        
        // 计算差异
        java.util.List<DiffUtil.DiffLine> baseToCurrent = DiffUtil.diff(baseContent, currentContent);
        java.util.List<DiffUtil.DiffLine> baseToDraft = DiffUtil.diff(baseContent, draftContent);
        
        // 简化的块生成逻辑：按段落分割，比较每个段落
        String[] baseParagraphs = baseContent.split("\n\n");
        String[] currentParagraphs = currentContent.split("\n\n");
        String[] draftParagraphs = draftContent.split("\n\n");
        
        int maxLen = Math.max(Math.max(baseParagraphs.length, currentParagraphs.length), draftParagraphs.length);
        int conflictCount = 0;
        int lineNum = 1;
        
        for (int i = 0; i < maxLen; i++) {
            String basePara = i < baseParagraphs.length ? baseParagraphs[i] : "";
            String currentPara = i < currentParagraphs.length ? currentParagraphs[i] : "";
            String draftPara = i < draftParagraphs.length ? draftParagraphs[i] : "";
            
            com.knowledge.api.dto.MergePreviewDTO.MergeBlock block = new com.knowledge.api.dto.MergePreviewDTO.MergeBlock();
            block.setBlockId(i);
            block.setBaseContent(basePara);
            block.setCurrentContent(currentPara);
            block.setDraftContent(draftPara);
            block.setStartLine(lineNum);
            
            int paraLines = Math.max(1, basePara.split("\n").length);
            lineNum += paraLines;
            block.setEndLine(lineNum - 1);
            
            if (basePara.equals(currentPara) && basePara.equals(draftPara)) {
                // 三方相同
                block.setType("EQUAL");
                block.setAutoMergedContent(basePara);
            } else if (basePara.equals(currentPara)) {
                // 只有草稿修改了
                block.setType("DRAFT_ONLY");
                block.setAutoMergedContent(draftPara);
            } else if (basePara.equals(draftPara)) {
                // 只有当前版本修改了
                block.setType("CURRENT_ONLY");
                block.setAutoMergedContent(currentPara);
            } else if (currentPara.equals(draftPara)) {
                // 两边做了相同的修改
                block.setType("EQUAL");
                block.setAutoMergedContent(currentPara);
            } else {
                // 两边都修改了，冲突
                block.setType("CONFLICT");
                block.setAutoMergedContent(null);
                conflictCount++;
            }
            
            blocks.add(block);
        }
        
        preview.setBlocks(blocks);
        preview.setHasConflict(conflictCount > 0);
        preview.setConflictBlockCount(conflictCount);
        
        if (conflictCount > 0) {
            preview.setMessage(String.format("检测到 %d 处冲突需要手动解决", conflictCount));
        } else {
            preview.setMessage("无冲突，可以自动合并");
        }
        
        log.info("合并预览生成完成: {} 个块, {} 处冲突", blocks.size(), conflictCount);
        return preview;
    }
    
    @Override
    @Transactional
    public KnowledgeDTO resolveMerge(com.knowledge.api.dto.MergeResolveRequest request) {
        log.info("解决合并冲突: knowledgeId={}, draftVersion={}", 
                request.getKnowledgeId(), request.getDraftVersion());
        
        String finalContent;
        
        if (request.getMergedContent() != null && !request.getMergedContent().isEmpty()) {
            // 如果前端直接提供了合并后的完整内容
            finalContent = request.getMergedContent();
        } else if (request.getResolutions() != null && !request.getResolutions().isEmpty()) {
            // 根据块解决方案构建最终内容
            com.knowledge.api.dto.MergePreviewDTO preview = getMergePreview(
                request.getKnowledgeId(),
                request.getBaseVersion(),
                request.getCurrentVersion(),
                request.getDraftVersion()
            );
            
            StringBuilder sb = new StringBuilder();
            for (com.knowledge.api.dto.MergePreviewDTO.MergeBlock block : preview.getBlocks()) {
                // 查找该块的解决方案
                String blockContent = block.getAutoMergedContent();
                
                for (com.knowledge.api.dto.MergeResolveRequest.BlockResolution resolution : request.getResolutions()) {
                    if (resolution.getBlockId() == block.getBlockId()) {
                        switch (resolution.getChoice()) {
                            case "CURRENT":
                                blockContent = block.getCurrentContent();
                                break;
                            case "DRAFT":
                                blockContent = block.getDraftContent();
                                break;
                            case "BOTH":
                                blockContent = block.getCurrentContent() + "\n\n" + block.getDraftContent();
                                break;
                            case "CUSTOM":
                                blockContent = resolution.getCustomContent();
                                break;
                        }
                        break;
                    }
                }
                
                if (blockContent != null && !blockContent.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append("\n\n");
                    }
                    sb.append(blockContent);
                }
            }
            finalContent = sb.toString();
        } else {
            // 没有冲突或强制覆盖，尝试自动合并
            com.knowledge.api.dto.MergeStatusDTO status = checkMergeStatus(
                request.getKnowledgeId(), 
                request.getDraftVersion()
            );
            
            if (status.isHasConflict() && !request.isForceOverwrite()) {
                throw new RuntimeException("存在冲突，请先解决冲突或选择强制覆盖");
            }
            
            finalContent = status.getMergedContent();
        }
        
        // 使用 mergeAndPublish 完成发布
        return mergeAndPublish(request.getKnowledgeId(), request.getDraftVersion(), finalContent);
    }

}

