package com.knowledge.api.service;

import com.knowledge.api.dto.ChunkUploadDTO;
import com.knowledge.api.dto.FileDTO;
import com.knowledge.api.dto.UploadResponseDTO;

import java.util.List;

public interface FileService {
    UploadResponseDTO initUpload(String fileName, Long fileSize, String fileHash);
    UploadResponseDTO uploadChunk(ChunkUploadDTO chunkUploadDTO);
    FileDTO completeUpload(String uploadId);
    FileDTO getFileById(Long fileId);
    List<FileDTO> batchDownload(List<Long> fileIds);
    boolean deleteFile(Long fileId);
    String getFilePreviewUrl(Long fileId);
}

