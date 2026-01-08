-- 清空除了用户和部门之外的所有业务数据

USE knowledge_db;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 清空知识相关表
TRUNCATE TABLE knowledge;
TRUNCATE TABLE knowledge_version;
TRUNCATE TABLE knowledge_branch;
TRUNCATE TABLE knowledge_relation;
TRUNCATE TABLE knowledge_comment;
TRUNCATE TABLE comment_like;
TRUNCATE TABLE audit;
TRUNCATE TABLE user_knowledge_collection;

-- 2. 清空文件相关表
TRUNCATE TABLE file_info;
TRUNCATE TABLE chunk_info;
TRUNCATE TABLE file_version;

-- 3. 清空积分日志 (可选，根据需求保留或删除，这里删除以重置系统状态)
TRUNCATE TABLE user_point_log;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'System cleaned successfully (Users and Departments preserved).' as result;
