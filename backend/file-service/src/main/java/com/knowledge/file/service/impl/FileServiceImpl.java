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

import javax.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Value("${file.upload.path:/data/files}")
    private String uploadPath;

    @Value("${file.chunk.path:/data/chunks}")
    private String chunkPath;

    @Override
    public UploadResponseDTO initUpload(String fileName, Long fileSize, String fileHash) {
        // 检查文件是否已存在（秒传）
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getFileHash, fileHash);
        FileInfo existingFile = fileInfoMapper.selectOne(wrapper);
        if (existingFile != null) {
            UploadResponseDTO response = new UploadResponseDTO();
            response.setCompleted(true);
            FileDTO fileDTO = new FileDTO();
            BeanUtils.copyProperties(existingFile, fileDTO);
            response.setFile(fileDTO);
            return response;
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
        
        redisTemplate.opsForValue().set(uploadKey, uploadInfo, 24, TimeUnit.HOURS);
        
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
            throw new RuntimeException("上传任务不存在或已过期");
        }

        // 验证分片哈希
        String calculatedHash = DigestUtil.sha256Hex(chunkUploadDTO.getChunkData());
        if (!calculatedHash.equals(chunkUploadDTO.getChunkHash())) {
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
            
            try {
                Files.createDirectories(chunkFilePath.getParent());
                Files.write(chunkFilePath, chunkUploadDTO.getChunkData());
            } catch (IOException e) {
                log.error("保存分片失败", e);
                throw new RuntimeException("保存分片失败", e);
            }

            // 保存分片信息
            ChunkInfo chunkInfo = new ChunkInfo();
            chunkInfo.setUploadId(chunkUploadDTO.getUploadId());
            chunkInfo.setChunkIndex(chunkUploadDTO.getChunkIndex());
            chunkInfo.setChunkHash(chunkUploadDTO.getChunkHash());
            chunkInfo.setChunkPath(chunkFilePath.toString());
            chunkInfoMapper.insert(chunkInfo);
        }

        // 更新已上传分片列表
        Set<Integer> uploadedChunks = uploadInfo.getUploadedChunks();
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
            throw new RuntimeException("上传任务不存在");
        }

        // 获取所有分片
        List<ChunkInfo> chunks = chunkInfoMapper.selectByUploadId(uploadId);
        if (chunks.size() != uploadInfo.getTotalChunks()) {
            throw new RuntimeException("分片不完整");
        }

        // 合并文件
        String fileExtension = uploadInfo.getFileName().substring(uploadInfo.getFileName().lastIndexOf("."));
        String finalFileName = uploadInfo.getFileHash() + fileExtension;
        Path finalFilePath = Paths.get(uploadPath, finalFileName);

        try {
            Files.createDirectories(finalFilePath.getParent());
            try (FileOutputStream fos = new FileOutputStream(finalFilePath.toFile())) {
                for (ChunkInfo chunk : chunks) {
                    byte[] chunkData = Files.readAllBytes(Paths.get(chunk.getChunkPath()));
                    fos.write(chunkData);
                }
            }

            // 验证文件哈希
            String finalHash = DigestUtil.sha256Hex(finalFilePath.toFile());
            if (!finalHash.equals(uploadInfo.getFileHash())) {
                Files.delete(finalFilePath);
                throw new RuntimeException("文件哈希校验失败");
            }

            // 保存文件信息
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(uploadInfo.getFileName());
            fileInfo.setFilePath(finalFilePath.toString());
            fileInfo.setFileType(detectFileType(uploadInfo.getFileName()));
            fileInfo.setFileSize(uploadInfo.getFileSize());
            fileInfo.setFileHash(uploadInfo.getFileHash());
            fileInfo.setStatus(Constants.FILE_STATUS_DRAFT);
            fileInfo.setCreateTime(LocalDateTime.now());
            fileInfoMapper.insert(fileInfo);

            // 清理分片
            for (ChunkInfo chunk : chunks) {
                Files.deleteIfExists(Paths.get(chunk.getChunkPath()));
                chunkInfoMapper.deleteById(chunk.getId());
            }
            redisTemplate.delete(uploadKey);

            FileDTO fileDTO = new FileDTO();
            BeanUtils.copyProperties(fileInfo, fileDTO);
            return fileDTO;

        } catch (IOException e) {
            log.error("合并文件失败", e);
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

