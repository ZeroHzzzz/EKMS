package com.knowledge.common.constant;

public class Constants {
    // 文件状态
    public static final String FILE_STATUS_DRAFT = "DRAFT"; // 草稿
    public static final String FILE_STATUS_PENDING = "PENDING"; // 待审核
    public static final String FILE_STATUS_APPROVED = "APPROVED"; // 已发布
    public static final String FILE_STATUS_REJECTED = "REJECTED"; // 已驳回
    public static final String FILE_STATUS_ARCHIVED = "ARCHIVED"; // 已归档

    // 审核状态
    public static final String AUDIT_STATUS_PENDING = "PENDING";
    public static final String AUDIT_STATUS_APPROVED = "APPROVED";
    public static final String AUDIT_STATUS_REJECTED = "REJECTED";

    // 文件类型
    public static final String FILE_TYPE_WORD = "WORD";
    public static final String FILE_TYPE_EXCEL = "EXCEL";
    public static final String FILE_TYPE_PPT = "PPT";
    public static final String FILE_TYPE_PDF = "PDF";
    public static final String FILE_TYPE_TXT = "TXT";
    public static final String FILE_TYPE_IMAGE = "IMAGE";
    public static final String FILE_TYPE_VIDEO = "VIDEO";
    public static final String FILE_TYPE_AUDIO = "AUDIO";

    // 分片大小 (5MB)
    public static final long CHUNK_SIZE = 5 * 1024 * 1024;

    // 权限
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_AUDITOR = "AUDITOR";
    public static final String ROLE_USER = "USER";
}

