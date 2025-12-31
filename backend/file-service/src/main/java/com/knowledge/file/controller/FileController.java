package com.knowledge.file.controller;

import com.knowledge.api.dto.FileDTO;
import com.knowledge.api.dto.UploadResponseDTO;
import com.knowledge.api.service.FileService;
import com.knowledge.common.result.Result;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @DubboReference
    private FileService fileService;

    @PostMapping("/init")
    public Result<UploadResponseDTO> initUpload(
            @RequestParam String fileName,
            @RequestParam Long fileSize,
            @RequestParam String fileHash) {
        UploadResponseDTO response = fileService.initUpload(fileName, fileSize, fileHash);
        return Result.success(response);
    }

    @PostMapping("/chunk")
    public Result<UploadResponseDTO> uploadChunk(
            @RequestParam String uploadId,
            @RequestParam Integer chunkIndex,
            @RequestParam String chunkHash,
            @RequestParam MultipartFile chunk) {
        try {
            com.knowledge.api.dto.ChunkUploadDTO chunkDTO = new com.knowledge.api.dto.ChunkUploadDTO();
            chunkDTO.setUploadId(uploadId);
            chunkDTO.setChunkIndex(chunkIndex);
            chunkDTO.setChunkHash(chunkHash);
            chunkDTO.setChunkData(chunk.getBytes());
            UploadResponseDTO response = fileService.uploadChunk(chunkDTO);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/complete/{uploadId}")
    public Result<FileDTO> completeUpload(@PathVariable String uploadId) {
        FileDTO fileDTO = fileService.completeUpload(uploadId);
        return Result.success(fileDTO);
    }

    @GetMapping("/{fileId}")
    public Result<FileDTO> getFile(@PathVariable Long fileId) {
        FileDTO fileDTO = fileService.getFileById(fileId);
        return Result.success(fileDTO);
    }

    @PostMapping("/batch/download")
    public Result<List<FileDTO>> batchDownload(@RequestBody List<Long> fileIds) {
        List<FileDTO> files = fileService.batchDownload(fileIds);
        return Result.success(files);
    }

    @DeleteMapping("/{fileId}")
    public Result<Boolean> deleteFile(@PathVariable Long fileId) {
        boolean result = fileService.deleteFile(fileId);
        return Result.success(result);
    }

    @GetMapping("/preview/{fileId}")
    public Result<String> getPreviewUrl(@PathVariable Long fileId) {
        String url = fileService.getFilePreviewUrl(fileId);
        return Result.success(url);
    }
}

