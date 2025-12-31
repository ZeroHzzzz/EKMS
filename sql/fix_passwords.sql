-- ============================================
-- 修复用户密码为BCrypt加密格式
-- ============================================
-- 所有测试账号的密码都是：password123
-- BCrypt hash: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pQ5O

USE knowledge_db;

-- 更新所有测试账号的密码为BCrypt加密的password123
UPDATE `user` SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pQ5O' 
WHERE `username` IN ('admin', 'editor1', 'editor2', 'editor3', 'user1', 'user2', 'user3', 'user4', 'user5');

-- 验证更新结果
SELECT `username`, `password`, LENGTH(`password`) as password_length, LEFT(`password`, 10) as password_prefix 
FROM `user` 
WHERE `username` IN ('admin', 'editor1', 'editor2', 'editor3', 'user1', 'user2', 'user3', 'user4', 'user5');

-- 说明：
-- 1. BCrypt hash应该以 $2a$ 或 $2b$ 开头
-- 2. 长度应该是60个字符
-- 3. 如果密码前缀不是 $2a$ 或 $2b$，说明是明文密码，需要更新

