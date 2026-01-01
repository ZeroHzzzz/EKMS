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

    // 评论状态
    public static final String COMMENT_STATUS_APPROVED = "APPROVED";
    public static final String COMMENT_STATUS_PENDING = "PENDING";
    public static final String COMMENT_STATUS_DELETED = "DELETED";

    // 关联类型
    public static final String RELATION_TYPE_RELATED = "RELATED";
    public static final String RELATION_TYPE_REFERENCE = "REFERENCE";
    public static final String RELATION_TYPE_SIMILAR = "SIMILAR";

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

    // 用户角色
    public static final String ROLE_ADMIN = "ADMIN";      // 总管理员（负责审核和管理）
    public static final String ROLE_EDITOR = "EDITOR";    // 各部门知识管理员（负责上传）
    public static final String ROLE_USER = "USER";         // 普通员工（查看、搜索、下载）

    // 权限代码
    public static final String PERMISSION_ALL = "ALL";
    public static final String PERMISSION_VIEW = "VIEW";
    public static final String PERMISSION_UPLOAD = "UPLOAD";
    public static final String PERMISSION_EDIT = "EDIT";
    public static final String PERMISSION_DELETE = "DELETE";
    public static final String PERMISSION_DOWNLOAD = "DOWNLOAD";
    public static final String PERMISSION_BATCH_UPLOAD = "BATCH_UPLOAD";
    public static final String PERMISSION_BATCH_DOWNLOAD = "BATCH_DOWNLOAD";
    public static final String PERMISSION_AUDIT = "AUDIT";
    public static final String PERMISSION_SUBMIT_AUDIT = "SUBMIT_AUDIT";
    public static final String PERMISSION_VIEW_AUDIT = "VIEW_AUDIT";
    public static final String PERMISSION_MANAGE_USER = "MANAGE_USER";
    public static final String PERMISSION_MANAGE_ROLE = "MANAGE_ROLE";
    public static final String PERMISSION_MANAGE_CONFIG = "MANAGE_CONFIG";
    public static final String PERMISSION_MANAGE_STRUCTURE = "MANAGE_STRUCTURE";
    public static final String PERMISSION_VIEW_STATISTICS = "VIEW_STATISTICS";
    public static final String PERMISSION_EXPORT_DATA = "EXPORT_DATA";
}

