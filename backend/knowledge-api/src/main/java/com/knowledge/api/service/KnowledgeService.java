package com.knowledge.api.service;

import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.KnowledgeQueryDTO;
import com.knowledge.api.dto.KnowledgeVersionDTO;
import com.knowledge.api.dto.StatisticsDTO;

import java.util.List;
import java.util.Map;

public interface KnowledgeService {
    KnowledgeDTO createKnowledge(KnowledgeDTO knowledgeDTO);
    KnowledgeDTO updateKnowledge(KnowledgeDTO knowledgeDTO);
    KnowledgeDTO getKnowledgeById(Long id);
    List<KnowledgeDTO> listKnowledge(KnowledgeQueryDTO queryDTO);
    boolean deleteKnowledge(Long id);
    void updateClickCount(Long id);
    void updateCollectCount(Long id, boolean collect);
    boolean publishKnowledge(Long id);
    
    /**
     * 发布指定版本
     * @param knowledgeId 知识ID
     * @param version 要发布的版本号
     * @return 是否成功
     */
    boolean publishVersion(Long knowledgeId, Long version);
    
    List<KnowledgeDTO> getHotKnowledge(int limit);
    List<KnowledgeDTO> getRelatedKnowledge(Long id, int limit);
    StatisticsDTO getStatistics();
    
    // 收藏相关方法
    boolean collectKnowledge(Long userId, Long knowledgeId);
    boolean cancelCollectKnowledge(Long userId, Long knowledgeId);
    
    // 批量操作
    boolean batchUpdateKnowledge(List<Long> knowledgeIds, Map<String, Object> updateData);
    
    // 知识树相关
    List<KnowledgeDTO> getKnowledgeTree();
    boolean moveKnowledge(Long knowledgeId, Long parentId, Integer sortOrder);
    List<KnowledgeDTO> getKnowledgePath(Long knowledgeId); // 获取知识路径（从根到当前节点）
    List<KnowledgeDTO> getChildren(Long parentId); // 获取子节点列表
    boolean isCollected(Long userId, Long knowledgeId);
    List<KnowledgeDTO> getMyCollections(Long userId);
    
    // 版本相关方法
    List<KnowledgeVersionDTO> getKnowledgeVersions(Long knowledgeId);
    
    /**
     * 根据用户权限获取知识版本列表
     * - 管理员：显示所有版本
     * - 普通用户：仅显示已发布版本 + 自己创建的未发布版本
     * @param knowledgeId 知识ID
     * @param username 当前用户名
     * @param isAdmin 是否是管理员
     * @return 过滤后的版本列表
     */
    List<KnowledgeVersionDTO> getKnowledgeVersionsForUser(Long knowledgeId, String username, boolean isAdmin);
    
    KnowledgeVersionDTO getKnowledgeVersion(Long knowledgeId, Long version);
    KnowledgeVersionDTO.DiffResult compareVersions(Long knowledgeId, Long version1, Long version2);
    
    /**
     * 回退到指定版本
     * @param knowledgeId 知识ID
     * @param targetVersion 目标版本号
     * @param operatorUsername 操作人用户名
     * @return 回退后的知识DTO
     */
    KnowledgeDTO revertToVersion(Long knowledgeId, Long targetVersion, String operatorUsername);
    
    /**
     * 根据文件ID获取关联的知识
     * @param fileId 文件ID
     * @return 知识DTO，如果不存在返回null
     */
    KnowledgeDTO getKnowledgeByFileId(Long fileId);
    
    /**
     * 从文件编辑创建新版本（用于OnlyOffice编辑后保存）
     * @param knowledgeId 知识ID
     * @param newFileId 新文件ID
     * @param operatorUsername 操作用户ID
     * @param operatorId 操作用户ID (可选)
     * @param changeDescription 变更说明
     * @return 更新后的知识DTO
     */
    KnowledgeDTO createVersionFromFileEdit(Long knowledgeId, Long newFileId, String operatorUsername, Long operatorId, String changeDescription);
    
    /**
     * 驳回指定版本（将版本状态更新为已驳回）
     * @param knowledgeId 知识ID
     * @param version 版本号
     * @return 是否成功
     */
    boolean rejectVersion(Long knowledgeId, Long version);
    
    /**
     * 检查版本合并状态
     * @param knowledgeId 知识ID
     * @param draftVersion 草稿版本号
     * @return 合并状态DTO
     */
    com.knowledge.api.dto.MergeStatusDTO checkMergeStatus(Long knowledgeId, Long draftVersion);

    /**
     * 合并并发布草稿（支持传入解决冲突后的内容）
     * @param knowledgeId 知识ID
     * @param draftVersion 草稿版本号
     * @param resolvedContent 解决冲突后的内容（如果为null，尝试自动合并）
     * @return 发布后的 KnowledgeDTO
     */
    KnowledgeDTO mergeAndPublish(Long knowledgeId, Long draftVersion, String resolvedContent);

    /**
     * 更新知识的整体状态（不创建新版本，仅更新状态标记）
     * @param knowledgeId 知识ID
     * @param status 新状态
     * @param hasDraft 是否有草稿
     * @return 是否成功
     */
    boolean updateKnowledgeStatus(Long knowledgeId, String status, boolean hasDraft);
    
    /**
     * 更新版本的提交信息
     * @param knowledgeId 知识ID
     * @param version 版本号
     * @param commitMessage 提交信息
     */
    void updateVersionCommitMessage(Long knowledgeId, Long version, String commitMessage);
    
    /**
     * 获取合并预览（GitHub风格的块级差异）
     * @param knowledgeId 知识ID
     * @param baseVersion 基础版本号
     * @param currentVersion 当前发布版本号
     * @param draftVersion 草稿版本号
     * @return 合并预览DTO，包含块级差异信息
     */
    com.knowledge.api.dto.MergePreviewDTO getMergePreview(Long knowledgeId, Long baseVersion, Long currentVersion, Long draftVersion);
    
    /**
     * 解决合并冲突并发布
     * @param request 合并解决请求
     * @return 发布后的知识DTO
     */
    KnowledgeDTO resolveMerge(com.knowledge.api.dto.MergeResolveRequest request);
}


