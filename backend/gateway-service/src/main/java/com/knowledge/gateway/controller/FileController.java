package com.knowledge.gateway.controller;

import com.knowledge.api.dto.FileDTO;
import com.knowledge.api.dto.UploadResponseDTO;
import com.knowledge.api.service.FileService;
import com.knowledge.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
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
            log.error("完成上传失败: uploadId={}, error={}", uploadId, e.getMessage(), e);
            // 返回更友好的错误信息
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("文件哈希校验失败")) {
                return Result.error("文件上传失败：文件哈希校验不通过，可能是文件传输过程中出现错误，请重新上传");
            } else if (errorMessage != null && errorMessage.contains("分片不完整")) {
                return Result.error("文件上传失败：分片不完整，请重新上传");
            } else if (errorMessage != null && errorMessage.contains("上传任务不存在")) {
                return Result.error("文件上传失败：上传任务已过期，请重新上传");
            } else {
                return Result.error("文件上传失败：" + (errorMessage != null ? errorMessage : "未知错误"));
            }
        }
    }

    @GetMapping("/check/{fileHash}")
    public Result<FileDTO> checkFileByHash(@PathVariable String fileHash) {
        try {
            // 通过文件哈希查找文件
            FileDTO fileDTO = fileService.getFileByHash(fileHash);
            // 文件不存在时返回成功但data为null，这是正常情况，不应该作为错误处理
            return Result.success(fileDTO);
        } catch (Exception e) {
            log.error("检查文件失败: fileHash={}", fileHash, e);
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
    public ResponseEntity<Resource> previewFile(@PathVariable Long fileId) {
        try {
            FileDTO fileDTO = fileService.getFileById(fileId);
            if (fileDTO == null || fileDTO.getFilePath() == null) {
                return ResponseEntity.notFound().build();
            }
            
            File file = new File(fileDTO.getFilePath());
            if (!file.exists()) {
                log.error("文件不存在: {}", fileDTO.getFilePath());
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(Paths.get(fileDTO.getFilePath()));
            if (contentType == null) {
                // 根据文件扩展名设置默认Content-Type
                String fileName = fileDTO.getFileName().toLowerCase();
                if (fileName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (fileName.endsWith(".txt")) {
                    contentType = "text/plain; charset=utf-8";
                } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                    contentType = "text/html; charset=utf-8";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            
            // 使用 inline 而不是 attachment，让浏览器直接预览而不是下载
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + java.net.URLEncoder.encode(fileDTO.getFileName(), "UTF-8") + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("预览文件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        try {
            FileDTO fileDTO = fileService.getFileById(fileId);
            if (fileDTO == null || fileDTO.getFilePath() == null) {
                return ResponseEntity.notFound().build();
            }
            
            File file = new File(fileDTO.getFilePath());
            if (!file.exists()) {
                log.error("文件不存在: {}", fileDTO.getFilePath());
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(Paths.get(fileDTO.getFilePath()));
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + java.net.URLEncoder.encode(fileDTO.getFileName(), "UTF-8") + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("下载文件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

