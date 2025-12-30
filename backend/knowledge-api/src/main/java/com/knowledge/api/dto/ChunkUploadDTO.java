package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ChunkUploadDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uploadId;
    private Integer chunkIndex;
    private String chunkHash;
    private byte[] chunkData;
    private Long totalChunks;
}

