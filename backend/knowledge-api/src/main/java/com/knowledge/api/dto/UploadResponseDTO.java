package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class UploadResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uploadId;
    private Boolean completed;
    private List<Integer> uploadedChunks;
    private FileDTO file;
}

