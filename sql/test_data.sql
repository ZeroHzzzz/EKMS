-- ============================================
-- 企业知识库管理系统 - 测试数据脚本
-- ============================================

USE knowledge_db;

-- ============================================
-- 1. 用户测试数据
-- ============================================

-- 清空现有测试用户（保留admin，如果admin已存在则使用INSERT IGNORE避免错误）
-- 只删除测试用户，不删除admin（如果admin已存在）
DELETE FROM `user` WHERE `username` IN (
    'editor1', 'editor2', 'editor3',
    'user1', 'user2', 'user3', 'user4', 'user5'
);

-- 插入测试用户（如果已存在则忽略，避免重复键错误）
-- 注意：密码使用BCrypt加密，所有测试账号的默认密码都是 'password123'
-- 如果需要生成新的密码hash，可以使用 PasswordUtil.java 工具类
INSERT IGNORE INTO `user` (`username`, `password`, `real_name`, `email`, `department`, `role`, `create_time`, `create_by`) VALUES
-- 总管理员（密码：password123）
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pQ5O', '系统管理员', 'admin@knowledge.com', 'IT部门', 'ADMIN', NOW(), 'system'),

-- 各部门知识管理员（密码：password123）
('editor1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pQ5O', '王编辑', 'editor1@knowledge.com', '业务部', 'EDITOR', NOW(), 'admin'),
('editor2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pQ5O', '赵编辑', 'editor2@knowledge.com', '技术部', 'EDITOR', NOW(), 'admin'),
('editor3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pQ5O', '钱编辑', 'editor3@knowledge.com', '客户服务部', 'EDITOR', NOW(), 'admin'),

-- 普通员工（密码：password123）
('user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pQ5O', '张三', 'user1@knowledge.com', '业务部', 'USER', NOW(), 'admin'),
('user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pQ5O', '李四', 'user2@knowledge.com', '业务部', 'USER', NOW(), 'admin'),
('user3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pQ5O', '王五', 'user3@knowledge.com', '技术部', 'USER', NOW(), 'admin'),
('user4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pQ5O', '赵六', 'user4@knowledge.com', '客户服务部', 'USER', NOW(), 'admin'),
('user5', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pQ5O', '钱七', 'user5@knowledge.com', '财务部', 'USER', NOW(), 'admin');

-- ============================================
-- 2. 文件信息测试数据
-- ============================================

-- 清空测试文件数据
DELETE FROM `file_info` WHERE `id` > 0;

INSERT IGNORE INTO `file_info` (`file_name`, `file_path`, `file_type`, `file_size`, `file_hash`, `status`, `upload_user_id`, `preview_url`, `create_time`, `create_by`) VALUES
('产品介绍手册.pdf', '/files/2024/01/product_manual.pdf', 'PDF', 2048576, 'abc123def456', 'APPROVED', 4, '/preview/files/2024/01/product_manual.pdf', DATE_SUB(NOW(), INTERVAL 30 DAY), 'editor1'),
('业务流程规范.docx', '/files/2024/01/business_process.docx', 'WORD', 1536000, 'def456ghi789', 'APPROVED', 4, '/preview/files/2024/01/business_process.docx', DATE_SUB(NOW(), INTERVAL 25 DAY), 'editor1'),
('系统操作指南.xlsx', '/files/2024/01/system_guide.xlsx', 'EXCEL', 1024000, 'ghi789jkl012', 'APPROVED', 5, '/preview/files/2024/01/system_guide.xlsx', DATE_SUB(NOW(), INTERVAL 20 DAY), 'editor2'),
('技术架构文档.pdf', '/files/2024/01/tech_architecture.pdf', 'PDF', 3072000, 'jkl012mno345', 'APPROVED', 5, '/preview/files/2024/01/tech_architecture.pdf', DATE_SUB(NOW(), INTERVAL 15 DAY), 'editor2'),
('客户服务流程.pptx', '/files/2024/01/service_process.pptx', 'PPT', 4096000, 'mno345pqr678', 'APPROVED', 6, '/preview/files/2024/01/service_process.pptx', DATE_SUB(NOW(), INTERVAL 10 DAY), 'editor3'),
('合规要求说明.pdf', '/files/2024/01/compliance.pdf', 'PDF', 2560000, 'pqr678stu901', 'PENDING', 4, '/preview/files/2024/01/compliance.pdf', DATE_SUB(NOW(), INTERVAL 5 DAY), 'editor1'),
('培训视频.mp4', '/files/2024/01/training_video.mp4', 'VIDEO', 52428800, 'stu901vwx234', 'APPROVED', 5, '/preview/files/2024/01/training_video.mp4', DATE_SUB(NOW(), INTERVAL 8 DAY), 'editor2'),
('常见问题解答.txt', '/files/2024/01/faq.txt', 'TXT', 512000, 'vwx234yza567', 'DRAFT', 6, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), 'editor3'),
('风险管理手册.pdf', '/files/2024/01/risk_management.pdf', 'PDF', 3584000, 'yza567bcd890', 'APPROVED', 4, '/preview/files/2024/01/risk_management.pdf', DATE_SUB(NOW(), INTERVAL 12 DAY), 'editor1'),
('数据统计报表.xlsx', '/files/2024/01/statistics.xlsx', 'EXCEL', 2048000, 'bcd890efg123', 'APPROVED', 5, '/preview/files/2024/01/statistics.xlsx', DATE_SUB(NOW(), INTERVAL 18 DAY), 'editor2');

-- ============================================
-- 3. 知识测试数据
-- ============================================

-- 清空测试知识数据
DELETE FROM `knowledge` WHERE `id` > 0;

INSERT IGNORE INTO `knowledge` (`title`, `content`, `summary`, `category`, `keywords`, `author`, `department`, `file_id`, `status`, `click_count`, `collect_count`, `version`, `parent_id`, `sort_order`, `create_time`, `create_by`) VALUES
-- 已发布的知识（热门）
('企业知识库产品介绍', 
 '企业知识库管理系统是一个集知识存储、检索、管理于一体的综合性平台。系统支持多种文件格式，包括Word、Excel、PDF、PPT、视频等，提供全文搜索、拼音搜索、首字母搜索等多种检索方式。系统具有完善的权限管理和审核流程，确保知识内容的质量和安全性。',
 '企业知识库管理系统产品介绍，包括系统功能、特点和使用方法。',
 '业务知识', '知识库,产品介绍,系统功能', '王编辑', '业务部', 1, 'APPROVED', 1250, 89, 1, NULL, 1, DATE_SUB(NOW(), INTERVAL 30 DAY), 'editor1'),

('业务流程规范说明', 
 '本文档详细说明了公司主要业务流程的操作规范，包括客户开户流程、资金划转流程、产品申购流程等。每个流程都包含详细的步骤说明、注意事项和常见问题解答。',
 '业务流程操作规范，涵盖开户、划转、申购等核心业务。',
 '业务知识', '业务流程,操作规范,开户,划转', '王编辑', '业务部', 2, 'APPROVED', 980, 67, 1, NULL, 2, DATE_SUB(NOW(), INTERVAL 25 DAY), 'editor1'),

('系统操作指南', 
 '系统操作指南详细介绍了知识库管理系统的各项功能使用方法，包括知识上传、编辑、搜索、下载等操作步骤。同时提供了常见问题的解决方案和操作技巧。',
 '知识库系统操作指南，帮助用户快速掌握系统使用方法。',
 '技术知识', '系统操作,使用指南,操作手册', '赵编辑', '技术部', 3, 'APPROVED', 1560, 112, 1, NULL, 3, DATE_SUB(NOW(), INTERVAL 20 DAY), 'editor2'),

('技术架构设计文档', 
 '本文档详细描述了企业知识库管理系统的技术架构设计，包括系统架构、数据库设计、接口设计、安全设计等方面。系统采用微服务架构，使用Spring Cloud、Dubbo等技术栈。',
 '系统技术架构设计文档，包含架构、数据库、接口等设计说明。',
 '技术知识', '技术架构,系统设计,微服务,Spring Cloud', '赵编辑', '技术部', 4, 'APPROVED', 890, 45, 1, NULL, 4, DATE_SUB(NOW(), INTERVAL 15 DAY), 'editor2'),

('客户服务流程标准', 
 '客户服务流程标准规定了客户服务人员在处理客户咨询、投诉、建议等各类服务请求时的标准流程和操作规范。包括服务响应时间、服务话术、问题处理流程等。',
 '客户服务标准流程和操作规范。',
 '客户服务', '客户服务,服务流程,服务标准', '钱编辑', '客户服务部', 5, 'APPROVED', 1340, 78, 1, NULL, 5, DATE_SUB(NOW(), INTERVAL 10 DAY), 'editor3'),

-- 待审核的知识
('合规要求说明', 
 '本文档详细说明了金融行业相关的合规要求，包括法律法规、监管要求、内部合规制度等。所有业务操作必须严格遵守相关合规要求。',
 '金融行业合规要求说明文档。',
 '合规知识', '合规,法律法规,监管要求', '王编辑', '业务部', 6, 'PENDING', 0, 0, 1, NULL, 6, DATE_SUB(NOW(), INTERVAL 5 DAY), 'editor1'),

-- 已发布的知识（视频类）
('新员工培训视频', 
 '新员工入职培训视频，内容包括公司介绍、企业文化、业务知识、系统操作等。视频时长约60分钟，建议新员工在入职第一周内完成观看。',
 '新员工入职培训视频，涵盖公司介绍、文化、业务和系统操作。',
 '管理知识', '培训,新员工,入职培训,视频', '赵编辑', '技术部', 7, 'APPROVED', 2100, 156, 1, NULL, 7, DATE_SUB(NOW(), INTERVAL 8 DAY), 'editor2'),

-- 草稿
('常见问题解答', 
 '本文档整理了知识库系统使用过程中的常见问题及解答，包括登录问题、搜索问题、上传问题、下载问题等。',
 '知识库系统常见问题解答。',
 '客户服务', '常见问题,FAQ,问题解答', '钱编辑', '客户服务部', 8, 'DRAFT', 0, 0, 1, NULL, 8, DATE_SUB(NOW(), INTERVAL 3 DAY), 'editor3'),

-- 已发布的知识
('风险管理手册', 
 '风险管理手册详细说明了公司风险管理体系，包括风险识别、风险评估、风险控制、风险监测等各个环节的管理要求和操作流程。',
 '公司风险管理体系和管理要求。',
 '合规知识', '风险管理,风险控制,风险监测', '王编辑', '业务部', 9, 'APPROVED', 720, 34, 1, NULL, 9, DATE_SUB(NOW(), INTERVAL 12 DAY), 'editor1'),

('月度数据统计报表', 
 '本月业务数据统计报表，包括客户数量、交易量、收入、支出等各项业务指标的统计和分析。',
 '月度业务数据统计报表。',
 '管理知识', '数据统计,业务报表,月度报表', '赵编辑', '技术部', 10, 'APPROVED', 450, 23, 1, NULL, 10, DATE_SUB(NOW(), INTERVAL 18 DAY), 'editor2');

-- ============================================
-- 4. 审核记录测试数据
-- ============================================

-- 清空测试审核数据
DELETE FROM `audit` WHERE `id` > 0;

INSERT IGNORE INTO `audit` (`knowledge_id`, `status`, `submit_user_id`, `auditor_id`, `comment`, `validation_errors`, `create_time`, `update_time`, `create_by`) VALUES
-- 已审核通过的记录（由总管理员admin审核）
(1, 'APPROVED', 2, 1, '内容完整，格式规范，符合发布要求。', NULL, DATE_SUB(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 28 DAY), 'editor1'),
(2, 'APPROVED', 2, 1, '业务流程说明清晰，操作步骤详细，审核通过。', NULL, DATE_SUB(NOW(), INTERVAL 23 DAY), DATE_SUB(NOW(), INTERVAL 23 DAY), 'editor1'),
(3, 'APPROVED', 3, 1, '操作指南内容准确，图文并茂，易于理解。', NULL, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), 'editor2'),
(4, 'APPROVED', 3, 1, '技术文档专业，架构设计合理，审核通过。', NULL, DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY), 'editor2'),
(5, 'APPROVED', 4, 1, '服务流程标准明确，符合公司要求。', NULL, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), 'editor3'),
(7, 'APPROVED', 3, 1, '培训视频内容完整，质量良好。', NULL, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 'editor2'),
(9, 'APPROVED', 2, 1, '风险管理手册内容全面，符合合规要求。', NULL, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 'editor1'),
(10, 'APPROVED', 3, 1, '数据统计准确，报表格式规范。', NULL, DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY), 'editor2'),

-- 待审核的记录（等待总管理员审核）
(6, 'PENDING', 2, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 'editor1');

-- ============================================
-- 5. 测试账号说明
-- ============================================

-- 所有测试账号的默认密码都是：password123
-- 如果使用BCrypt加密，密码hash为：$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pQ5O
-- 
-- 测试账号列表：
-- 管理员：admin / password123
-- 审核员：auditor1 / password123, auditor2 / password123
-- 编辑员：editor1 / password123, editor2 / password123, editor3 / password123
-- 普通用户：user1 / password123, user2 / password123, user3 / password123, user4 / password123, user5 / password123

-- ============================================
-- 6. 数据统计说明
-- ============================================

-- 已创建：
-- - 9个用户账号（1个总管理员，3个知识管理员，5个普通员工）
-- - 10个文件记录（包含PDF、Word、Excel、PPT、视频、文本等格式）
-- - 10条知识记录（8条已发布，1条待审核，1条草稿）
-- - 9条审核记录（8条已通过，1条待审核）

-- 知识分类分布：
-- - 业务知识：3条
-- - 技术知识：3条
-- - 客户服务：2条
-- - 合规知识：2条
-- - 管理知识：2条

-- 热门知识（按点击率）：
-- 1. 新员工培训视频（2100次）
-- 2. 系统操作指南（1560次）
-- 3. 客户服务流程标准（1340次）
-- 4. 企业知识库产品介绍（1250次）
-- 5. 业务流程规范说明（980次）

