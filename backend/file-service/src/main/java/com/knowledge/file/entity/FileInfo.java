package com.knowledge.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_info")
public class FileInfo extends BaseEntity {
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String fileHash;
    private String status;
    private Long uploadUserId;
    private String previewUrl;
}

