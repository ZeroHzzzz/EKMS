package com.knowledge.gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.dto.FileDTO;
import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.service.AuditService;
import com.knowledge.api.service.FileService;
import com.knowledge.api.service.KnowledgeService;
import com.knowledge.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OnlyOffice 集成控制器
 * 处理文档编辑器配置和回调
 */
@Slf4j
@RestController
@RequestMapping("/api/onlyoffice")
public class OnlyOfficeController {

    @DubboReference(check = false, timeout = 10000)
    private FileService fileService;
    
    @DubboReference(check = false, timeout = 10000)
    private KnowledgeService knowledgeService;
    
    @DubboReference(check = false, timeout = 10000)
    private AuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // OnlyOffice Document Server 地址（从容器内部访问）
    @Value("${onlyoffice.document-server.url:http://onlyoffice}")
    private String documentServerUrl;

    // 对外访问的 OnlyOffice 地址（浏览器访问）
    @Value("${onlyoffice.document-server.external-url:http://localhost:8013}")
    private String documentServerExternalUrl;

    // 后端服务地址（供 OnlyOffice 回调）
    @Value("${onlyoffice.callback.url:http://host.docker.internal:8080}")
    private String callbackBaseUrl;

    // 文件下载地址（供 OnlyOffice 获取文件）
    @Value("${onlyoffice.file-download.url:http://host.docker.internal:8080}")
    private String fileDownloadUrl;

    /**
     * 获取文档编辑器配置
     * @param fileId 文件ID
     * @param mode 模式：edit（编辑）或 view（查看）
     * @param userName 当前用户名
     * @param userId 当前用户ID
     * @return 编辑器配置
     */
    @GetMapping("/config/{fileId}")
    public Result<Map<String, Object>> getEditorConfig(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "edit") String mode,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String userId) {
        try {
            FileDTO fileDTO = fileService.getFileById(fileId);
            if (fileDTO == null) {
                return Result.error("文件不存在");
            }

            String fileName = fileDTO.getFileName();
            String fileType = getFileExtension(fileName);
            String documentType = getDocumentType(fileType);

            if (documentType == null) {
                return Result.error("不支持的文件类型: " + fileType);
            }

            // 生成唯一的文档 key（用于 OnlyOffice 识别文档）
            String documentKey = generateDocumentKey(fileId, fileDTO.getCreateTime());

            // 构建配置
            Map<String, Object> config = new HashMap<>();
            
            // 文档配置
            Map<String, Object> document = new HashMap<>();
            document.put("fileType", fileType);
            document.put("key", documentKey);
            document.put("title", fileName);
            document.put("url", fileDownloadUrl + "/api/file/download/" + fileId);
            
            // 文档权限
            Map<String, Object> permissions = new HashMap<>();
            permissions.put("comment", true);
            permissions.put("download", true);
            permissions.put("edit", "edit".equals(mode));
            permissions.put("print", true);
            permissions.put("review", true);
            document.put("permissions", permissions);
            
            config.put("document", document);
            config.put("documentType", documentType);

            // 编辑器配置
            Map<String, Object> editorConfig = new HashMap<>();
            
            // 回调地址（文档保存时 OnlyOffice 会调用）
            // 添加用户信息参数，用于版本管理
            String callbackUrl = callbackBaseUrl + "/api/onlyoffice/callback?fileId=" + fileId;
            if (userName != null && !userName.isEmpty()) {
                callbackUrl += "&userName=" + java.net.URLEncoder.encode(userName, "UTF-8");
            }
            if (userId != null && !userId.isEmpty()) {
                callbackUrl += "&userId=" + userId;
            }
            editorConfig.put("callbackUrl", callbackUrl);
            
            // 语言设置
            editorConfig.put("lang", "zh-CN");
            
            // 编辑模式
            editorConfig.put("mode", mode);
            
            // 用户信息
            if (userName != null || userId != null) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", userId != null ? userId : "anonymous");
                user.put("name", userName != null ? userName : "匿名用户");
                editorConfig.put("user", user);
            }
            
            // 自定义设置
            Map<String, Object> customization = new HashMap<>();
            customization.put("autosave", true);
            customization.put("chat", false);
            customization.put("comments", true);
            customization.put("compactHeader", false);
            customization.put("compactToolbar", false);
            customization.put("feedback", false);
            customization.put("forcesave", true);
            customization.put("help", false);
            customization.put("hideRightMenu", false);
            customization.put("hideRulers", false);
            customization.put("submitForm", false);
            customization.put("about", false);
            
            // Logo 配置（可选）
            Map<String, Object> logo = new HashMap<>();
            logo.put("image", "");
            logo.put("imageEmbedded", "");
            logo.put("url", "");
            customization.put("logo", logo);
            
            editorConfig.put("customization", customization);
            
            config.put("editorConfig", editorConfig);
            
            // OnlyOffice API 地址
            config.put("documentServerUrl", documentServerExternalUrl);
            config.put("apiUrl", documentServerExternalUrl + "/web-apps/apps/api/documents/api.js");

            return Result.success(config);
        } catch (Exception e) {
            log.error("获取 OnlyOffice 配置失败: fileId={}", fileId, e);
            return Result.error("获取编辑器配置失败: " + e.getMessage());
        }
    }

    /**
     * OnlyOffice 回调接口
     * 当文档状态变化时（如保存），OnlyOffice 会调用此接口
     */
    @PostMapping("/callback")
    public Map<String, Object> callback(
            @RequestParam Long fileId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Long userId,
            @RequestBody String body) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("OnlyOffice 回调: fileId={}, userName={}, userId={}, body={}", fileId, userName, userId, body);
            
            JsonNode json = objectMapper.readTree(body);
            int status = json.get("status").asInt();
            
            /*
             * OnlyOffice 状态码：
             * 0 - 文档未找到
             * 1 - 文档正在编辑
             * 2 - 文档准备保存（可以下载）
             * 3 - 文档保存出错
             * 4 - 文档关闭，无修改
             * 6 - 文档正在编辑，但当前状态已保存
             * 7 - 强制保存文档时出错
             */
            
            if (status == 2 || status == 6) {
                // 文档准备保存或强制保存
                String downloadUrl = json.get("url").asText();
                log.info("下载编辑后的文档: fileId={}, url={}", fileId, downloadUrl);
                
                // 创建新版本（保存为新文件）
                boolean saved = createNewVersionFromEdit(fileId, downloadUrl, userName, userId);
                if (!saved) {
                    log.error("创建新版本失败: fileId={}", fileId);
                    response.put("error", 1);
                    return response;
                }
                
                log.info("文档保存成功并创建新版本: fileId={}", fileId);
            } else if (status == 4) {
                log.info("文档关闭，无修改: fileId={}", fileId);
            } else {
                log.info("文档状态: fileId={}, status={}", fileId, status);
            }
            
            response.put("error", 0);
        } catch (Exception e) {
            log.error("处理 OnlyOffice 回调失败: fileId={}", fileId, e);
            response.put("error", 1);
        }
        
        return response;
    }
    
    /**
     * 从文件编辑创建新版本
     * @param fileId 原文件ID
     * @param downloadUrl OnlyOffice提供的下载URL
     * @param userName 操作用户名
     * @param userId 操作用户ID
     * @return 是否成功
     */
    private boolean createNewVersionFromEdit(Long fileId, String downloadUrl, String userName, Long userId) {
        try {
            // 1. 根据文件ID获取关联的知识
            KnowledgeDTO knowledge = knowledgeService.getKnowledgeByFileId(fileId);
            if (knowledge == null) {
                log.warn("找不到文件关联的知识，回退到直接保存: fileId={}", fileId);
                // 如果没有关联的知识，直接更新原文件
                return downloadAndSaveFile(fileId, downloadUrl);
            }
            
            // 2. 保存编辑后的文件为新文件
            FileDTO newFile = fileService.saveEditedFile(fileId, downloadUrl);
            if (newFile == null) {
                log.error("保存编辑后的文件失败: fileId={}", fileId);
                return false;
            }
            
            // 3. 创建新版本
            String operatorUsername = userName != null ? userName : "系统";
            String changeDescription = "通过OnlyOffice编辑更新";
            
            KnowledgeDTO updatedKnowledge = knowledgeService.createVersionFromFileEdit(
                    knowledge.getId(), newFile.getId(), operatorUsername, changeDescription);
            
            if (updatedKnowledge == null) {
                log.error("创建新版本失败: knowledgeId={}", knowledge.getId());
                return false;
            }
            
            // 4. 如果有待审核草稿，自动提交审核
            // 注意：已发布文章编辑后，status可能仍是APPROVED，但hasDraft为true
            Boolean hasDraft = updatedKnowledge.getHasDraft();
            if (hasDraft != null && hasDraft) {
                try {
                    Long submitUserId = userId != null ? userId : 1L; // 默认使用管理员ID
                    auditService.submitForAudit(updatedKnowledge.getId(), submitUserId);
                    log.info("自动提交审核: knowledgeId={}, userId={}, hasDraft={}", 
                            updatedKnowledge.getId(), submitUserId, hasDraft);
                } catch (Exception e) {
                    log.warn("自动提交审核失败（不影响保存）: knowledgeId={}", updatedKnowledge.getId(), e);
                }
            }
            
            log.info("文件编辑创建新版本成功: fileId={}, knowledgeId={}, newFileId={}, newVersion={}", 
                    fileId, knowledge.getId(), newFile.getId(), updatedKnowledge.getVersion());
            return true;
            
        } catch (Exception e) {
            log.error("创建新版本失败: fileId={}", fileId, e);
            return false;
        }
    }

    /**
     * 强制保存文档
     * @param fileId 文件ID
     * @return 保存结果
     */
    @PostMapping("/forcesave/{fileId}")
    public Result<Boolean> forceSave(@PathVariable Long fileId) {
        try {
            FileDTO fileDTO = fileService.getFileById(fileId);
            if (fileDTO == null) {
                return Result.error("文件不存在");
            }
            
            String documentKey = generateDocumentKey(fileId, fileDTO.getCreateTime());
            
            // 调用 OnlyOffice 命令服务强制保存
            String commandUrl = documentServerUrl + "/coauthoring/CommandService.ashx";
            
            Map<String, Object> command = new HashMap<>();
            command.put("c", "forcesave");
            command.put("key", documentKey);
            
            log.info("发送强制保存命令: fileId={}, key={}, url={}", fileId, documentKey, commandUrl);
            
            // 发送HTTP POST请求到OnlyOffice命令服务
            URL url = new URL(commandUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            
            // 写入请求体
            String jsonBody = objectMapper.writeValueAsString(command);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes("UTF-8"));
            }
            
            // 读取响应
            int responseCode = conn.getResponseCode();
            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseBody.append(line);
                }
            }
            
            conn.disconnect();
            
            log.info("强制保存响应: fileId={}, responseCode={}, body={}", fileId, responseCode, responseBody);
            
            // 解析响应
            if (responseCode == 200) {
                JsonNode responseJson = objectMapper.readTree(responseBody.toString());
                int error = responseJson.has("error") ? responseJson.get("error").asInt() : -1;
                if (error == 0) {
                    return Result.success(true);
                } else {
                    // error 4 表示没有文档正在编辑，这也是成功的情况
                    if (error == 4) {
                        return Result.success(true);
                    }
                    return Result.error("OnlyOffice返回错误码: " + error);
                }
            } else {
                return Result.error("OnlyOffice服务响应异常: " + responseCode);
            }
            
        } catch (Exception e) {
            log.error("强制保存失败: fileId={}", fileId, e);
            return Result.error("强制保存失败: " + e.getMessage());
        }
    }

    /**
     * 检查 OnlyOffice 服务状态
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> checkHealth() {
        Map<String, Object> result = new HashMap<>();
        try {
            URL url = new URL(documentServerExternalUrl + "/healthcheck");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            result.put("status", responseCode == 200 ? "healthy" : "unhealthy");
            result.put("documentServerUrl", documentServerExternalUrl);
            result.put("responseCode", responseCode);
            
            conn.disconnect();
            return Result.success(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("documentServerUrl", documentServerExternalUrl);
            return Result.success(result);
        }
    }

    /**
     * 获取支持的文件类型
     */
    @GetMapping("/supported-types")
    public Result<Map<String, Object>> getSupportedTypes() {
        Map<String, Object> types = new HashMap<>();
        
        types.put("word", new String[]{"doc", "docx", "docm", "dot", "dotx", "dotm", "odt", "fodt", "ott", "rtf", "txt", "html", "htm", "mht", "xml", "pdf", "djvu", "fb2", "epub", "xps", "oxps"});
        types.put("cell", new String[]{"xls", "xlsx", "xlsm", "xlsb", "xlt", "xltx", "xltm", "ods", "fods", "ots", "csv"});
        types.put("slide", new String[]{"ppt", "pptx", "pptm", "pps", "ppsx", "ppsm", "pot", "potx", "potm", "odp", "fodp", "otp"});
        
        // 可编辑的文件类型
        types.put("editable", new String[]{"docx", "xlsx", "pptx", "txt"});
        
        return Result.success(types);
    }

    /**
     * 下载编辑后的文件并保存
     */
    private boolean downloadAndSaveFile(Long fileId, String downloadUrl) {
        try {
            FileDTO fileDTO = fileService.getFileById(fileId);
            if (fileDTO == null || fileDTO.getFilePath() == null) {
                log.error("文件信息不存在: fileId={}", fileId);
                return false;
            }
            
            File targetFile = new File(fileDTO.getFilePath());
            
            // 创建备份
            File backupFile = new File(fileDTO.getFilePath() + ".bak");
            if (targetFile.exists()) {
                Files.copy(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 下载新文件
            URL url = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            
            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            conn.disconnect();
            
            // 删除备份
            if (backupFile.exists()) {
                backupFile.delete();
            }
            
            // 更新文件大小
            fileService.updateFileSize(fileId, targetFile.length());
            
            log.info("文件保存成功: fileId={}, size={}", fileId, targetFile.length());
            return true;
        } catch (Exception e) {
            log.error("下载并保存文件失败: fileId={}, url={}", fileId, downloadUrl, e);
            return false;
        }
    }

    /**
     * 生成文档 Key
     * Key 必须唯一，当文档内容改变时 Key 也需要改变
     */
    private String generateDocumentKey(Long fileId, java.time.LocalDateTime uploadTime) {
        String timeStr = uploadTime != null ? String.valueOf(uploadTime.toEpochSecond(java.time.ZoneOffset.UTC)) : "0";
        return "file_" + fileId + "_" + timeStr;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 根据文件类型获取文档类型
     * @return word/cell/slide 或 null（不支持）
     */
    private String getDocumentType(String fileType) {
        // Word 文档
        if (fileType.matches("doc|docx|docm|dot|dotx|dotm|odt|fodt|ott|rtf|txt|html|htm|mht|xml|pdf|djvu|fb2|epub|xps|oxps")) {
            return "word";
        }
        // Excel 表格
        if (fileType.matches("xls|xlsx|xlsm|xlsb|xlt|xltx|xltm|ods|fods|ots|csv")) {
            return "cell";
        }
        // PPT 演示
        if (fileType.matches("ppt|pptx|pptm|pps|ppsx|ppsm|pot|potx|potm|odp|fodp|otp")) {
            return "slide";
        }
        return null;
    }
}

