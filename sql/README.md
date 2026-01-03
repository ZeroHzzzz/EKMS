# SQL 脚本说明

## 主要脚本

### `init.sql` - 数据库初始化脚本（主要脚本）
**用途：** 创建完整的数据库结构，包含所有表和字段  
**使用场景：** 
- 首次部署时初始化数据库
- 通过 Docker 自动执行（docker-compose.yml 已配置）
- 手动执行：`mysql -uroot -proot < sql/init.sql`

**包含内容：**
- 所有业务表（user, knowledge, file_info, audit 等）
- 所有扩展表（knowledge_version, knowledge_comment, knowledge_relation 等）
- 所有索引和约束
- 完整的字段定义（包括 Git 版本管理相关字段）

### `test_data.sql` - 测试数据脚本
**用途：** 插入测试数据（用户、文件、知识等）  
**使用场景：** 开发测试环境  
**注意：** 所有测试账号密码为 `password123`

### `clear_all_data.sql` - 清空所有数据脚本
**用途：** 清空所有业务数据，保留表结构  
**使用场景：** 重置测试环境  
**注意：** 会删除所有数据，请谨慎使用！

### `mysql-schema.sql` - Nacos 数据库初始化脚本
**用途：** 初始化 Nacos 配置中心的数据库  
**使用场景：** 通过 Docker 自动执行

### `fix_passwords.sql` - 修复密码格式脚本
**用途：** 将明文密码更新为 BCrypt 加密格式  
**使用场景：** 数据迁移或修复密码格式

### `reinit_database.sh` - 重新初始化数据库脚本
**用途：** 一键删除并重新创建数据库  
**使用场景：** 完全重置数据库环境  
**注意：** 会删除所有数据！

## 历史脚本（已归档）

以下脚本已合并到 `init.sql`，保留在 `archive/` 目录作为参考：

- `add_version_table.sql` - 知识版本表（已合并）
- `add_git_version_system.sql` - Git 版本管理字段（已合并）
- `add_relation_and_comment_tables.sql` - 关联和评论表（已合并）
- `add_collection_table.sql` - 收藏表（已合并）
- `add_knowledge_tree_indexes.sql` - 知识树索引（已合并）

## 使用建议

1. **首次部署：** 只需执行 `init.sql`（Docker 会自动执行）
2. **添加测试数据：** 执行 `test_data.sql`
3. **重置环境：** 执行 `reinit_database.sh` 或 `clear_all_data.sql`
4. **修复数据：** 根据需要执行相应的修复脚本

## 注意事项

- 所有脚本都使用 `IF NOT EXISTS` 或 `IF EXISTS` 确保可重复执行
- 生产环境请谨慎使用清空或重置脚本
- 建议在执行重要操作前备份数据库

