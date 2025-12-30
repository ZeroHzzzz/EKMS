package com.knowledge.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("chunk_info")
public class ChunkInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String uploadId;
    private Integer chunkIndex;
    private String chunkHash;
    private String chunkPath;
    private Long fileId;
}

