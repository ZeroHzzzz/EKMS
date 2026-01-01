package com.knowledge.api.service;

import com.knowledge.api.dto.CommentDTO;
import java.util.List;

public interface CommentService {
    CommentDTO addComment(Long knowledgeId, Long userId, Long parentId, String content);
    boolean deleteComment(Long commentId, Long userId);
    List<CommentDTO> getComments(Long knowledgeId, Long currentUserId);
    boolean likeComment(Long commentId, Long userId);
    boolean cancelLikeComment(Long commentId, Long userId);
}

