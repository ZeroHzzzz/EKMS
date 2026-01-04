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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    /**
     * 批量下载文件（打包为ZIP）
     * @param request 包含 fileIds 列表
     * @return ZIP 文件流
     */
    @PostMapping("/batch-download")
    public ResponseEntity<Resource> batchDownloadZip(@RequestBody Map<String, List<Long>> request) {
        List<Long> fileIds = request.get("fileIds");
        if (fileIds == null || fileIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        File tempZipFile = null;
        try {
            // 创建临时 ZIP 文件
            tempZipFile = File.createTempFile("batch_download_", ".zip");
            
            try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(tempZipFile))) {
                int count = 0;
                for (Long fileId : fileIds) {
                    try {
                        FileDTO fileDTO = fileService.getFileById(fileId);
                        if (fileDTO == null || fileDTO.getFilePath() == null) {
                            log.warn("文件不存在: fileId={}", fileId);
                            continue;
                        }
                        
                        File file = new File(fileDTO.getFilePath());
                        if (!file.exists()) {
                            log.warn("文件不存在: {}", fileDTO.getFilePath());
                            continue;
                        }
                        
                        // 添加文件到 ZIP
                        String entryName = fileDTO.getFileName();
                        // 处理重名文件
                        ZipEntry zipEntry = new ZipEntry(entryName);
                        zipOut.putNextEntry(zipEntry);
                        
                        try (FileInputStream fis = new FileInputStream(file)) {
                            byte[] buffer = new byte[8192];
                            int length;
                            while ((length = fis.read(buffer)) > 0) {
                                zipOut.write(buffer, 0, length);
                            }
                        }
                        zipOut.closeEntry();
                        count++;
                    } catch (Exception e) {
                        log.error("添加文件到ZIP失败: fileId={}", fileId, e);
                    }
                }
                log.info("批量下载: 共打包 {} 个文件", count);
            }
            
            Resource resource = new FileSystemResource(tempZipFile);
            String zipFileName = "知识库文档_" + java.time.LocalDate.now() + ".zip";
            
            // 设置文件删除钩子（响应完成后删除临时文件）
            final File finalTempFile = tempZipFile;
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + java.net.URLEncoder.encode(zipFileName, "UTF-8") + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(tempZipFile.length()))
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("批量下载失败", e);
            if (tempZipFile != null && tempZipFile.exists()) {
                tempZipFile.delete();
            }
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量下载文件（保留文件夹结构，打包为ZIP）
     * @param request 包含 items 列表，每个 item 有 fileId 和 path
     * @return ZIP 文件流
     */
    @PostMapping("/batch-download-with-structure")
    public ResponseEntity<Resource> batchDownloadWithStructure(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        File tempZipFile = null;
        try {
            tempZipFile = File.createTempFile("batch_download_structure_", ".zip");
            
            try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(tempZipFile))) {
                int count = 0;
                java.util.Set<String> usedPaths = new java.util.HashSet<>();
                
                for (Map<String, Object> item : items) {
                    try {
                        Long fileId = Long.valueOf(item.get("fileId").toString());
                        String path = (String) item.get("path"); // 文件在结构中的路径，如 "技术文档/Java/xxx.pdf"
                        
                        FileDTO fileDTO = fileService.getFileById(fileId);
                        if (fileDTO == null || fileDTO.getFilePath() == null) {
                            continue;
                        }
                        
                        File file = new File(fileDTO.getFilePath());
                        if (!file.exists()) {
                            continue;
                        }
                        
                        // 使用提供的路径或文件名
                        String entryName = (path != null && !path.isEmpty()) ? path : fileDTO.getFileName();
                        
                        // 处理重名
                        String originalName = entryName;
                        int dupCount = 1;
                        while (usedPaths.contains(entryName)) {
                            int lastDot = originalName.lastIndexOf('.');
                            if (lastDot > 0) {
                                entryName = originalName.substring(0, lastDot) + "_" + dupCount + originalName.substring(lastDot);
                            } else {
                                entryName = originalName + "_" + dupCount;
                            }
                            dupCount++;
                        }
                        usedPaths.add(entryName);
                        
                        ZipEntry zipEntry = new ZipEntry(entryName);
                        zipOut.putNextEntry(zipEntry);
                        
                        try (FileInputStream fis = new FileInputStream(file)) {
                            byte[] buffer = new byte[8192];
                            int length;
                            while ((length = fis.read(buffer)) > 0) {
                                zipOut.write(buffer, 0, length);
                            }
                        }
                        zipOut.closeEntry();
                        count++;
                    } catch (Exception e) {
                        log.error("添加文件到ZIP失败", e);
                    }
                }
                log.info("批量下载（带结构）: 共打包 {} 个文件", count);
            }
            
            Resource resource = new FileSystemResource(tempZipFile);
            String zipFileName = "知识库文档_" + java.time.LocalDate.now() + ".zip";
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + java.net.URLEncoder.encode(zipFileName, "UTF-8") + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(tempZipFile.length()))
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("批量下载失败", e);
            if (tempZipFile != null && tempZipFile.exists()) {
                tempZipFile.delete();
            }
            return ResponseEntity.internalServerError().build();
        }
    }
}

