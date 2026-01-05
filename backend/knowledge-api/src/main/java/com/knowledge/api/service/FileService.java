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
    FileDTO getFileByHash(String fileHash);
    List<FileDTO> batchDownload(List<Long> fileIds);
    boolean deleteFile(Long fileId);
    String getFilePreviewUrl(Long fileId);
    String getFilePath(Long fileId);
    
    /**
     * 更新文件大小（OnlyOffice 编辑后调用）
     * @param fileId 文件ID
     * @param newSize 新的文件大小
     * @return 是否更新成功
     */
    boolean updateFileSize(Long fileId, Long newSize);
}

