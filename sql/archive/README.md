# 历史 SQL 脚本归档

此目录包含已合并到 `init.sql` 的历史脚本，保留作为参考。

## 已归档的脚本

1. **add_version_table.sql** - 知识版本历史表
   - 已合并到 `init.sql` 的 `knowledge_version` 表定义

2. **add_git_version_system.sql** - Git 版本管理系统
   - 已合并到 `init.sql`：
     - `knowledge` 表的 `current_branch` 和 `current_commit_hash` 字段
     - `knowledge_version` 表的 Git 相关字段
     - `knowledge_branch` 表
     - `file_version` 表

3. **add_relation_and_comment_tables.sql** - 关联和评论功能
   - 已合并到 `init.sql`：
     - `knowledge_relation` 表
     - `knowledge_comment` 表
     - `comment_like` 表

4. **add_collection_table.sql** - 用户收藏功能
   - 已合并到 `init.sql` 的 `user_knowledge_collection` 表

5. **add_knowledge_tree_indexes.sql** - 知识树索引优化
   - 已合并到 `init.sql` 的 `knowledge` 表索引定义

## 说明

这些脚本已不再需要单独执行，因为 `init.sql` 已经包含了所有内容。
保留这些脚本仅作为历史参考，了解数据库结构的演进过程。

