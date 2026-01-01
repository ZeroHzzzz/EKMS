package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long knowledgeId;
    private Long userId;
    private String userName;
    private String userRealName;
    private Long parentId;
    private String content;
    private String status;
    private Integer likeCount;
    private Boolean isLiked; // 当前用户是否已点赞
    private LocalDateTime createTime;
    private List<CommentDTO> replies; // 回复列表
}

