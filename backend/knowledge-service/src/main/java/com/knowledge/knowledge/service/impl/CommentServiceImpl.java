package com.knowledge.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.api.dto.CommentDTO;
import com.knowledge.api.dto.UserDTO;
import com.knowledge.api.service.CommentService;
import com.knowledge.api.service.UserService;
import com.knowledge.common.constant.Constants;
import com.knowledge.knowledge.entity.CommentLike;
import com.knowledge.knowledge.entity.KnowledgeComment;
import com.knowledge.knowledge.mapper.CommentLikeMapper;
import com.knowledge.knowledge.mapper.KnowledgeCommentMapper;
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

@Slf4j
@Service
@DubboService
public class CommentServiceImpl implements CommentService {

    @Resource
    private KnowledgeCommentMapper commentMapper;

    @Resource
    private CommentLikeMapper likeMapper;

    @DubboReference(check = false, timeout = 10000)
    private UserService userService;

    @Override
    @Transactional
    public CommentDTO addComment(Long knowledgeId, Long userId, Long parentId, String content) {
        KnowledgeComment comment = new KnowledgeComment();
        comment.setKnowledgeId(knowledgeId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setStatus(Constants.COMMENT_STATUS_APPROVED); // 默认已通过，可以后续添加审核
        comment.setLikeCount(0);
        comment.setCreateTime(LocalDateTime.now());
        
        commentMapper.insert(comment);
        
        CommentDTO dto = new CommentDTO();
        BeanUtils.copyProperties(comment, dto);
        
        // 加载用户信息
        UserDTO user = userService.getUserById(userId);
        if (user != null) {
            dto.setUserName(user.getUsername());
            dto.setUserRealName(user.getRealName());
        }
        
        return dto;
    }

    @Override
    @Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        KnowledgeComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return false;
        }
        
        // 检查权限：只能删除自己的评论
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此评论");
        }
        
        // 软删除：更新状态为DELETED
        comment.setStatus(Constants.COMMENT_STATUS_DELETED);
        comment.setUpdateTime(LocalDateTime.now());
        commentMapper.updateById(comment);
        
        return true;
    }

    @Override
    public List<CommentDTO> getComments(Long knowledgeId, Long currentUserId) {
        LambdaQueryWrapper<KnowledgeComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeComment::getKnowledgeId, knowledgeId);
        wrapper.eq(KnowledgeComment::getStatus, Constants.COMMENT_STATUS_APPROVED);
        wrapper.orderByDesc(KnowledgeComment::getCreateTime);
        
        List<KnowledgeComment> comments = commentMapper.selectList(wrapper);
        
        // 转换为DTO并加载用户信息
        List<CommentDTO> commentDTOs = comments.stream().map(comment -> {
            CommentDTO dto = new CommentDTO();
            BeanUtils.copyProperties(comment, dto);
            
            // 加载用户信息
            UserDTO user = userService.getUserById(comment.getUserId());
            if (user != null) {
                dto.setUserName(user.getUsername());
                dto.setUserRealName(user.getRealName());
            }
            
            // 检查当前用户是否已点赞
            if (currentUserId != null) {
                LambdaQueryWrapper<CommentLike> likeWrapper = new LambdaQueryWrapper<>();
                likeWrapper.eq(CommentLike::getCommentId, comment.getId());
                likeWrapper.eq(CommentLike::getUserId, currentUserId);
                dto.setIsLiked(likeMapper.selectCount(likeWrapper) > 0);
            }
            
            return dto;
        }).collect(Collectors.toList());
        
        // 构建树形结构
        return buildCommentTree(commentDTOs);
    }

    @Override
    @Transactional
    public boolean likeComment(Long commentId, Long userId) {
        // 检查是否已点赞
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getCommentId, commentId);
        wrapper.eq(CommentLike::getUserId, userId);
        
        if (likeMapper.selectCount(wrapper) > 0) {
            return false; // 已点赞
        }
        
        // 添加点赞
        CommentLike like = new CommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        likeMapper.insert(like);
        
        // 更新评论点赞数
        KnowledgeComment comment = commentMapper.selectById(commentId);
        if (comment != null) {
            comment.setLikeCount((comment.getLikeCount() == null ? 0 : comment.getLikeCount()) + 1);
            commentMapper.updateById(comment);
        }
        
        return true;
    }

    @Override
    @Transactional
    public boolean cancelLikeComment(Long commentId, Long userId) {
        // 删除点赞
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getCommentId, commentId);
        wrapper.eq(CommentLike::getUserId, userId);
        
        int deleted = likeMapper.delete(wrapper);
        
        if (deleted > 0) {
            // 更新评论点赞数
            KnowledgeComment comment = commentMapper.selectById(commentId);
            if (comment != null && comment.getLikeCount() != null && comment.getLikeCount() > 0) {
                comment.setLikeCount(comment.getLikeCount() - 1);
                commentMapper.updateById(comment);
            }
            return true;
        }
        
        return false;
    }

    /**
     * 构建评论树形结构
     */
    private List<CommentDTO> buildCommentTree(List<CommentDTO> comments) {
        // 按parentId分组
        Map<Long, List<CommentDTO>> replyMap = comments.stream()
            .filter(c -> c.getParentId() != null)
            .collect(Collectors.groupingBy(CommentDTO::getParentId));
        
        // 构建树形结构
        List<CommentDTO> rootComments = new ArrayList<>();
        for (CommentDTO comment : comments) {
            if (comment.getParentId() == null) {
                // 根评论
                comment.setReplies(replyMap.getOrDefault(comment.getId(), new ArrayList<>()));
                rootComments.add(comment);
            }
        }
        
        return rootComments;
    }
    @Override
    @Transactional
    public void deleteByKnowledgeId(Long knowledgeId) {
        // 1. 查找该知识下所有评论ID
        LambdaQueryWrapper<KnowledgeComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeComment::getKnowledgeId, knowledgeId);
        queryWrapper.select(KnowledgeComment::getId);
        List<Object> commentIds = commentMapper.selectObjs(queryWrapper);
        
        if (commentIds != null && !commentIds.isEmpty()) {
            List<Long> ids = commentIds.stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(Collectors.toList());
                
            // 2. 删除这些评论的点赞记录
            LambdaQueryWrapper<CommentLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.in(CommentLike::getCommentId, ids);
            likeMapper.delete(likeWrapper);
            
            // 3. 删除评论
            commentMapper.deleteBatchIds(ids);
            log.info("已删除知识 ID={} 关联的 {} 条评论及其点赞记录", knowledgeId, ids.size());
        }
    }
}

