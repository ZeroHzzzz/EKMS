package com.knowledge.gateway.controller;

import com.knowledge.api.dto.FileDTO;
import com.knowledge.api.dto.UploadResponseDTO;
import com.knowledge.api.service.FileService;
import com.knowledge.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {

    @DubboReference(check = false, timeout = 10000)
    private FileService fileService;

    @PostMapping("/init")
    public Result<UploadResponseDTO> initUpload(
            @RequestParam String fileName,
            @RequestParam Long fileSize,
            @RequestParam String fileHash) {
        try {
            UploadResponseDTO response = fileService.initUpload(fileName, fileSize, fileHash);
            return Result.success(response);
        } catch (Exception e) {
            log.error("初始化上传失败", e);
            return Result.error(e.getMessage());
        }
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
            log.error("上传分片失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/complete/{uploadId}")
    public Result<FileDTO> completeUpload(@PathVariable String uploadId) {
        try {
            FileDTO fileDTO = fileService.completeUpload(uploadId);
            return Result.success(fileDTO);
        } catch (Exception e) {
            log.error("完成上传失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{fileId}")
    public Result<FileDTO> getFile(@PathVariable Long fileId) {
        try {
            FileDTO fileDTO = fileService.getFileById(fileId);
            return Result.success(fileDTO);
        } catch (Exception e) {
            log.error("获取文件失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/batch/download")
    public Result<List<FileDTO>> batchDownload(@RequestBody List<Long> fileIds) {
        try {
            List<FileDTO> files = fileService.batchDownload(fileIds);
            return Result.success(files);
        } catch (Exception e) {
            log.error("批量下载失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{fileId}")
    public Result<Boolean> deleteFile(@PathVariable Long fileId) {
        try {
            boolean result = fileService.deleteFile(fileId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/preview/{fileId}")
    public Result<String> getPreviewUrl(@PathVariable Long fileId) {
        try {
            String url = fileService.getFilePreviewUrl(fileId);
            return Result.success(url);
        } catch (Exception e) {
            log.error("获取预览URL失败", e);
            return Result.error(e.getMessage());
        }
    }
}

