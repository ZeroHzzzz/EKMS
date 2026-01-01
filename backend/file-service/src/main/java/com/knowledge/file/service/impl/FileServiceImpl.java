package com.knowledge.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.api.dto.*;
import com.knowledge.api.service.FileService;
import com.knowledge.common.constant.Constants;
import com.knowledge.file.entity.ChunkInfo;
import com.knowledge.file.entity.FileInfo;
import com.knowledge.file.mapper.ChunkInfoMapper;
import com.knowledge.file.mapper.FileInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@DubboService
public class FileServiceImpl implements FileService {

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private ChunkInfoMapper chunkInfoMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${file.upload.path:./uploads/files}")
    private String uploadPath;

    @Value("${file.chunk.path:./uploads/chunks}")
    private String chunkPath;
    
    @PostConstruct
    public void init() {
        // 将相对路径转换为绝对路径（相对于项目根目录）
        // 项目根目录是java目录
        
        // 首先检查环境变量或系统属性是否指定了项目根目录
        String projectRootEnv = System.getenv("PROJECT_ROOT");
        if (projectRootEnv == null || projectRootEnv.isEmpty()) {
            projectRootEnv = System.getProperty("project.root");
        }
        
        String projectRoot;
        if (projectRootEnv != null && !projectRootEnv.isEmpty()) {
            projectRoot = projectRootEnv;
            log.info("使用环境变量/系统属性指定的项目根目录: {}", projectRoot);
        } else {
            String currentDir = System.getProperty("user.dir");
            log.info("当前工作目录: {}", currentDir);
            // 查找项目根目录（包含uploads目录的目录）
            projectRoot = findProjectRoot(currentDir);
            log.info("自动查找的项目根目录: {}", projectRoot);
        }
        
        // 如果路径是相对路径（以./开头），转换为绝对路径
        if (uploadPath.startsWith("./") || (!java.nio.file.Paths.get(uploadPath).isAbsolute())) {
            // 移除开头的 ./
            String relativePath = uploadPath.replaceFirst("^\\./", "");
            uploadPath = new java.io.File(projectRoot, relativePath).getAbsolutePath();
        }
        
        if (chunkPath.startsWith("./") || (!java.nio.file.Paths.get(chunkPath).isAbsolute())) {
            String relativePath = chunkPath.replaceFirst("^\\./", "");
            chunkPath = new java.io.File(projectRoot, relativePath).getAbsolutePath();
        }
        
        // 创建目录
        try {
            java.nio.file.Path uploadDir = java.nio.file.Paths.get(uploadPath);
            java.nio.file.Path chunkDir = java.nio.file.Paths.get(chunkPath);
            
            java.nio.file.Files.createDirectories(uploadDir);
            java.nio.file.Files.createDirectories(chunkDir);
            
            // 验证目录是否真的创建成功
            if (!java.nio.file.Files.exists(uploadDir) || !java.nio.file.Files.isDirectory(uploadDir)) {
                throw new RuntimeException("文件上传目录创建失败: " + uploadDir.toAbsolutePath());
            }
            if (!java.nio.file.Files.exists(chunkDir) || !java.nio.file.Files.isDirectory(chunkDir)) {
                throw new RuntimeException("分片目录创建失败: " + chunkDir.toAbsolutePath());
            }
            
            log.info("文件上传目录已初始化: {} (绝对路径: {})", uploadPath, uploadDir.toAbsolutePath());
            log.info("分片目录已初始化: {} (绝对路径: {})", chunkPath, chunkDir.toAbsolutePath());
        } catch (Exception e) {
            log.error("初始化上传目录失败: uploadPath={}, chunkPath={}", uploadPath, chunkPath, e);
            throw new RuntimeException("初始化上传目录失败", e);
        }
    }
    
    /**
     * 查找项目根目录（包含uploads目录的目录）
     * 从当前目录开始向上查找，直到找到包含uploads目录的目录，或者找到backend目录的父目录
     */
    private String findProjectRoot(String startDir) {
        java.io.File current = new java.io.File(startDir);
        int maxDepth = 10; // 最多向上查找10级，防止无限循环
        
        for (int i = 0; i < maxDepth; i++) {
            // 检查当前目录是否包含uploads目录
            java.io.File uploadsDir = new java.io.File(current, "uploads");
            if (uploadsDir.exists() && uploadsDir.isDirectory()) {
                return current.getAbsolutePath();
            }
            
            // 如果当前目录是backend/file-service，向上两级到项目根目录
            String path = current.getAbsolutePath();
            if (path.endsWith("file-service")) {
                java.io.File parent = current.getParentFile();
                if (parent != null) {
                    java.io.File grandParent = parent.getParentFile();
                    if (grandParent != null) {
                        return grandParent.getAbsolutePath();
                    }
                }
            } else if (path.endsWith("backend")) {
                java.io.File parent = current.getParentFile();
                if (parent != null) {
                    return parent.getAbsolutePath();
                }
            }
            
            // 向上查找
            java.io.File parent = current.getParentFile();
            if (parent == null || parent.equals(current)) {
                break;
            }
            current = parent;
        }
        
        // 如果找不到，返回当前目录
        log.warn("未能找到项目根目录，使用当前目录: {}", startDir);
        return startDir;
    }

    @Override
    public UploadResponseDTO initUpload(String fileName, Long fileSize, String fileHash) {
        log.info("初始化上传: fileName={}, fileSize={}, fileHash={}", fileName, fileSize, fileHash);
        
        // 检查文件是否已存在（秒传）
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getFileHash, fileHash);
        FileInfo existingFile = fileInfoMapper.selectOne(wrapper);
        if (existingFile != null) {
            // 检查文件是否真的存在
            String filePath = existingFile.getFilePath();
            Path filePathObj;
            
            // 如果是相对路径，转换为绝对路径
            if (filePath.startsWith("./") || (!Paths.get(filePath).isAbsolute())) {
                // 移除开头的 ./
                String relativePath = filePath.replaceFirst("^\\./", "");
                // uploadPath 已经是绝对路径，比如 /mnt/c/.../uploads/files
                // 我们需要找到项目根目录，然后拼接相对路径
                // 如果 relativePath 是 uploads/files/xxx.txt，我们需要项目根目录/uploads/files/xxx.txt
                Path uploadPathObj = Paths.get(uploadPath);
                // uploadPath 是 uploads/files，所以项目根目录是 uploadPath 的父目录的父目录
                Path projectRoot = uploadPathObj.getParent().getParent();
                filePathObj = projectRoot.resolve(relativePath);
            } else {
                filePathObj = Paths.get(filePath);
            }
            
            boolean fileExists = Files.exists(filePathObj);
            log.info("文件已存在（秒传）: fileId={}, fileName={}, dbPath={}, actualPath={}, 文件存在={}", 
                    existingFile.getId(), existingFile.getFileName(), filePath, 
                    filePathObj.toAbsolutePath(), fileExists);
            
            if (!fileExists) {
                log.warn("文件记录存在但文件不存在，删除记录: fileId={}, path={}", 
                        existingFile.getId(), filePathObj.toAbsolutePath());
                // 文件不存在，删除记录，允许重新上传
                fileInfoMapper.deleteById(existingFile.getId());
            } else {
                // 如果数据库中的路径是相对路径，更新为绝对路径
                if (filePath.startsWith("./") || (!Paths.get(filePath).isAbsolute())) {
                    existingFile.setFilePath(filePathObj.toAbsolutePath().toString());
                    fileInfoMapper.updateById(existingFile);
                    log.info("更新文件路径为绝对路径: fileId={}, oldPath={}, newPath={}", 
                            existingFile.getId(), filePath, existingFile.getFilePath());
                }
                
                UploadResponseDTO response = new UploadResponseDTO();
                response.setCompleted(true);
                FileDTO fileDTO = new FileDTO();
                BeanUtils.copyProperties(existingFile, fileDTO);
                response.setFile(fileDTO);
                return response;
            }
        }

        // 创建上传任务
        String uploadId = IdUtil.simpleUUID();
        String uploadKey = "upload:" + uploadId;
        
        UploadInfo uploadInfo = new UploadInfo();
        uploadInfo.setUploadId(uploadId);
        uploadInfo.setFileName(fileName);
        uploadInfo.setFileSize(fileSize);
        uploadInfo.setFileHash(fileHash);
        uploadInfo.setTotalChunks((int) ((fileSize + Constants.CHUNK_SIZE - 1) / Constants.CHUNK_SIZE));
        uploadInfo.setUploadedChunks(new HashSet<>()); // 初始化为空集合
        
        redisTemplate.opsForValue().set(uploadKey, uploadInfo, 24, TimeUnit.HOURS);
        
        log.info("创建上传任务: uploadId={}, fileName={}, totalChunks={}, chunkPath={}, uploadPath={}", 
                uploadId, fileName, uploadInfo.getTotalChunks(), chunkPath, uploadPath);
        
        UploadResponseDTO response = new UploadResponseDTO();
        response.setUploadId(uploadId);
        response.setCompleted(false);
        response.setUploadedChunks(new ArrayList<>());
        
        return response;
    }

    @Override
    @Transactional
    public UploadResponseDTO uploadChunk(ChunkUploadDTO chunkUploadDTO) {
        String uploadKey = "upload:" + chunkUploadDTO.getUploadId();
        UploadInfo uploadInfo = (UploadInfo) redisTemplate.opsForValue().get(uploadKey);
        
        if (uploadInfo == null) {
            log.error("上传任务不存在或已过期: uploadId={}", chunkUploadDTO.getUploadId());
            throw new RuntimeException("上传任务不存在或已过期");
        }

        log.info("开始上传分片: uploadId={}, chunkIndex={}, chunkSize={}", 
                chunkUploadDTO.getUploadId(), chunkUploadDTO.getChunkIndex(), 
                chunkUploadDTO.getChunkData() != null ? chunkUploadDTO.getChunkData().length : 0);

        // 验证分片哈希
        String calculatedHash = DigestUtil.sha256Hex(chunkUploadDTO.getChunkData());
        if (!calculatedHash.equals(chunkUploadDTO.getChunkHash())) {
            log.error("分片哈希校验失败: uploadId={}, chunkIndex={}, expected={}, actual={}", 
                    chunkUploadDTO.getUploadId(), chunkUploadDTO.getChunkIndex(),
                    chunkUploadDTO.getChunkHash(), calculatedHash);
            throw new RuntimeException("分片哈希校验失败");
        }

        // 检查分片是否已上传
        ChunkInfo existingChunk = chunkInfoMapper.selectOne(
            new LambdaQueryWrapper<ChunkInfo>()
                .eq(ChunkInfo::getUploadId, chunkUploadDTO.getUploadId())
                .eq(ChunkInfo::getChunkIndex, chunkUploadDTO.getChunkIndex())
        );

        if (existingChunk == null) {
            // 保存分片
            String chunkFileName = chunkUploadDTO.getUploadId() + "_" + chunkUploadDTO.getChunkIndex();
            Path chunkFilePath = Paths.get(chunkPath, chunkFileName);
            
            log.info("保存分片到: {}", chunkFilePath.toAbsolutePath());
            
            try {
                Files.createDirectories(chunkFilePath.getParent());
                Files.write(chunkFilePath, chunkUploadDTO.getChunkData());
                
                // 验证文件是否真的保存了
                if (Files.exists(chunkFilePath)) {
                    long fileSize = Files.size(chunkFilePath);
                    log.info("分片保存成功: path={}, size={}", chunkFilePath.toAbsolutePath(), fileSize);
                } else {
                    log.error("分片保存失败: 文件不存在 path={}", chunkFilePath.toAbsolutePath());
                    throw new RuntimeException("分片保存失败: 文件不存在");
                }
            } catch (IOException e) {
                log.error("保存分片失败: path={}", chunkFilePath.toAbsolutePath(), e);
                throw new RuntimeException("保存分片失败", e);
            }

            // 保存分片信息
            ChunkInfo chunkInfo = new ChunkInfo();
            chunkInfo.setUploadId(chunkUploadDTO.getUploadId());
            chunkInfo.setChunkIndex(chunkUploadDTO.getChunkIndex());
            chunkInfo.setChunkHash(chunkUploadDTO.getChunkHash());
            chunkInfo.setChunkPath(chunkFilePath.toAbsolutePath().toString());
            chunkInfoMapper.insert(chunkInfo);
            log.info("分片信息已保存到数据库: id={}, path={}", chunkInfo.getId(), chunkInfo.getChunkPath());
        } else {
            log.info("分片已存在，跳过保存: uploadId={}, chunkIndex={}", 
                    chunkUploadDTO.getUploadId(), chunkUploadDTO.getChunkIndex());
        }

        // 更新已上传分片列表
        Set<Integer> uploadedChunks = uploadInfo.getUploadedChunks();
        if (uploadedChunks == null) {
            uploadedChunks = new HashSet<>();
        }
        uploadedChunks.add(chunkUploadDTO.getChunkIndex());
        uploadInfo.setUploadedChunks(uploadedChunks);
        redisTemplate.opsForValue().set(uploadKey, uploadInfo, 24, TimeUnit.HOURS);

        UploadResponseDTO response = new UploadResponseDTO();
        response.setUploadId(chunkUploadDTO.getUploadId());
        response.setCompleted(uploadedChunks.size() == uploadInfo.getTotalChunks());
        response.setUploadedChunks(new ArrayList<>(uploadedChunks));

        return response;
    }

    @Override
    @Transactional
    public FileDTO completeUpload(String uploadId) {
        String uploadKey = "upload:" + uploadId;
        UploadInfo uploadInfo = (UploadInfo) redisTemplate.opsForValue().get(uploadKey);
        
        if (uploadInfo == null) {
            log.error("上传任务不存在: uploadId={}", uploadId);
            throw new RuntimeException("上传任务不存在");
        }

        log.info("开始合并文件: uploadId={}, fileName={}, totalChunks={}", 
                uploadId, uploadInfo.getFileName(), uploadInfo.getTotalChunks());

        // 获取所有分片
        List<ChunkInfo> chunks = chunkInfoMapper.selectByUploadId(uploadId);
        log.info("获取到分片数量: {}, 期望数量: {}", chunks.size(), uploadInfo.getTotalChunks());
        
        if (chunks.size() != uploadInfo.getTotalChunks()) {
            log.error("分片不完整: uploadId={}, 实际={}, 期望={}", 
                    uploadId, chunks.size(), uploadInfo.getTotalChunks());
            throw new RuntimeException("分片不完整");
        }

        // 合并文件
        String fileExtension = uploadInfo.getFileName().substring(uploadInfo.getFileName().lastIndexOf("."));
        String finalFileName = uploadInfo.getFileHash() + fileExtension;
        Path finalFilePath = Paths.get(uploadPath, finalFileName);
        
        log.info("合并文件到: {}", finalFilePath.toAbsolutePath());

        try {
            Files.createDirectories(finalFilePath.getParent());
            long totalSize = 0;
            try (FileOutputStream fos = new FileOutputStream(finalFilePath.toFile())) {
                for (ChunkInfo chunk : chunks) {
                    Path chunkPath = Paths.get(chunk.getChunkPath());
                    if (!Files.exists(chunkPath)) {
                        log.error("分片文件不存在: path={}", chunkPath.toAbsolutePath());
                        throw new RuntimeException("分片文件不存在: " + chunkPath);
                    }
                    byte[] chunkData = Files.readAllBytes(chunkPath);
                    fos.write(chunkData);
                    totalSize += chunkData.length;
                    log.debug("合并分片: index={}, size={}", chunk.getChunkIndex(), chunkData.length);
                }
            }
            
            log.info("文件合并完成: path={}, size={}", finalFilePath.toAbsolutePath(), totalSize);

            // 验证文件是否存在
            if (!Files.exists(finalFilePath)) {
                log.error("合并后的文件不存在: path={}", finalFilePath.toAbsolutePath());
                throw new RuntimeException("合并后的文件不存在");
            }

            // 验证文件哈希
            String finalHash = DigestUtil.sha256Hex(finalFilePath.toFile());
            if (!finalHash.equals(uploadInfo.getFileHash())) {
                log.error("文件哈希校验失败: uploadId={}, fileName={}, expected={}, actual={}, fileSize={}", 
                        uploadId, uploadInfo.getFileName(), uploadInfo.getFileHash(), finalHash, totalSize);
                // 清理合并的文件
                try {
                    Files.deleteIfExists(finalFilePath);
                } catch (IOException e) {
                    log.warn("删除合并文件失败: {}", finalFilePath, e);
                }
                // 清理所有分片
                for (ChunkInfo chunk : chunks) {
                    try {
                        Path chunkPath = Paths.get(chunk.getChunkPath());
                        Files.deleteIfExists(chunkPath);
                        chunkInfoMapper.deleteById(chunk.getId());
                    } catch (Exception e) {
                        log.warn("清理分片失败: chunkId={}", chunk.getId(), e);
                    }
                }
                // 清理Redis中的上传任务
                redisTemplate.delete(uploadKey);
                throw new RuntimeException("文件哈希校验失败: 期望哈希=" + uploadInfo.getFileHash() + ", 实际哈希=" + finalHash);
            }
            
            log.info("文件哈希校验通过: uploadId={}, hash={}", uploadId, finalHash);

            // 保存文件信息
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(uploadInfo.getFileName());
            fileInfo.setFilePath(finalFilePath.toAbsolutePath().toString());
            fileInfo.setFileType(detectFileType(uploadInfo.getFileName()));
            fileInfo.setFileSize(uploadInfo.getFileSize());
            fileInfo.setFileHash(uploadInfo.getFileHash());
            fileInfo.setStatus(Constants.FILE_STATUS_DRAFT);
            fileInfo.setCreateTime(LocalDateTime.now());
            fileInfoMapper.insert(fileInfo);
            
            log.info("文件信息已保存到数据库: id={}, fileName={}, filePath={}", 
                    fileInfo.getId(), fileInfo.getFileName(), fileInfo.getFilePath());

            // 清理分片
            for (ChunkInfo chunk : chunks) {
                Path chunkPath = Paths.get(chunk.getChunkPath());
                if (Files.exists(chunkPath)) {
                    Files.deleteIfExists(chunkPath);
                    log.debug("删除分片: path={}", chunkPath.toAbsolutePath());
                }
                chunkInfoMapper.deleteById(chunk.getId());
            }
            redisTemplate.delete(uploadKey);
            
            log.info("文件上传完成: uploadId={}, fileId={}, path={}", 
                    uploadId, fileInfo.getId(), fileInfo.getFilePath());

            FileDTO fileDTO = new FileDTO();
            BeanUtils.copyProperties(fileInfo, fileDTO);
            return fileDTO;

        } catch (IOException e) {
            log.error("合并文件失败: uploadId={}, path={}", uploadId, finalFilePath.toAbsolutePath(), e);
            throw new RuntimeException("合并文件失败", e);
        }
    }

    @Override
    public FileDTO getFileById(Long fileId) {
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo == null) {
            return null;
        }
        FileDTO fileDTO = new FileDTO();
        BeanUtils.copyProperties(fileInfo, fileDTO);
        return fileDTO;
    }

    @Override
    public FileDTO getFileByHash(String fileHash) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getFileHash, fileHash);
        FileInfo fileInfo = fileInfoMapper.selectOne(wrapper);
        if (fileInfo == null) {
            return null;
        }
        FileDTO fileDTO = new FileDTO();
        BeanUtils.copyProperties(fileInfo, fileDTO);
        return fileDTO;
    }

    @Override
    public List<FileDTO> batchDownload(List<Long> fileIds) {
        List<FileInfo> fileInfos = fileInfoMapper.selectBatchIds(fileIds);
        return fileInfos.stream().map(fileInfo -> {
            FileDTO fileDTO = new FileDTO();
            BeanUtils.copyProperties(fileInfo, fileDTO);
            return fileDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean deleteFile(Long fileId) {
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo != null) {
            try {
                Files.deleteIfExists(Paths.get(fileInfo.getFilePath()));
                fileInfoMapper.deleteById(fileId);
                return true;
            } catch (IOException e) {
                log.error("删除文件失败", e);
                return false;
            }
        }
        return false;
    }

    @Override
    public String getFilePreviewUrl(Long fileId) {
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo != null) {
            return "/api/file/preview/" + fileId;
        }
        return null;
    }

    private String detectFileType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
        switch (extension) {
            case "DOC":
            case "DOCX":
                return Constants.FILE_TYPE_WORD;
            case "XLS":
            case "XLSX":
                return Constants.FILE_TYPE_EXCEL;
            case "PPT":
            case "PPTX":
                return Constants.FILE_TYPE_PPT;
            case "PDF":
                return Constants.FILE_TYPE_PDF;
            case "TXT":
                return Constants.FILE_TYPE_TXT;
            case "JPG":
            case "JPEG":
            case "PNG":
            case "GIF":
                return Constants.FILE_TYPE_IMAGE;
            case "MP4":
            case "AVI":
            case "MOV":
                return Constants.FILE_TYPE_VIDEO;
            case "MP3":
            case "WAV":
                return Constants.FILE_TYPE_AUDIO;
            default:
                return "UNKNOWN";
        }
    }

    // 内部类：上传信息
    private static class UploadInfo implements Serializable {
        private String uploadId;
        private String fileName;
        private Long fileSize;
        private String fileHash;
        private Integer totalChunks;
        private Set<Integer> uploadedChunks;

        // getters and setters
        public String getUploadId() { return uploadId; }
        public void setUploadId(String uploadId) { this.uploadId = uploadId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public String getFileHash() { return fileHash; }
        public void setFileHash(String fileHash) { this.fileHash = fileHash; }
        public Integer getTotalChunks() { return totalChunks; }
        public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }
        public Set<Integer> getUploadedChunks() { return uploadedChunks; }
        public void setUploadedChunks(Set<Integer> uploadedChunks) { this.uploadedChunks = uploadedChunks; }
    }
}

