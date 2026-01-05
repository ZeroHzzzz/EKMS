package com.knowledge.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FileDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String fileHash;
    private String status;
    private Long uploadUserId;
    private LocalDateTime createTime;
    private String previewUrl;
}

