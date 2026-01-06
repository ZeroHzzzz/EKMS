package com.knowledge.gateway.controller;

import com.knowledge.api.dto.CommentDTO;
import com.knowledge.api.service.CommentService;
import com.knowledge.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/comment")
public class CommentController {

    @DubboReference(check = false, timeout = 10000)
    private CommentService commentService;

    @PostMapping
    public Result<CommentDTO> addComment(@RequestBody AddCommentRequest request) {
        try {
            CommentDTO result = commentService.addComment(
                request.getKnowledgeId(),
                request.getUserId(),
                request.getParentId(),
                request.getContent()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("添加评论失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{commentId:\\d+}")
    public Result<Boolean> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        try {
            boolean result = commentService.deleteComment(commentId, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("删除评论失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/knowledge/{knowledgeId:\\d+}")
    public Result<List<CommentDTO>> getComments(
            @PathVariable Long knowledgeId,
            @RequestParam(required = false) Long userId) {
        try {
            List<CommentDTO> result = commentService.getComments(knowledgeId, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取评论列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{commentId:\\d+}/like")
    public Result<Boolean> likeComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        try {
            boolean result = commentService.likeComment(commentId, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("点赞评论失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{commentId:\\d+}/like")
    public Result<Boolean> cancelLikeComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        try {
            boolean result = commentService.cancelLikeComment(commentId, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("取消点赞失败", e);
            return Result.error(e.getMessage());
        }
    }

    // 内部类：添加评论请求
    @lombok.Data
    public static class AddCommentRequest {
        private Long knowledgeId;
        private Long userId;
        private Long parentId;
        private String content;
    }
}

