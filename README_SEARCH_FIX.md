# 知识库搜索问题修复指南

## 问题描述
知识库搜索页面无法搜索到任何内容。

## 原因分析
这种情况通常是因为 Elasticsearch 索引为空或与数据库不同步导致的。
1. 如果是通过 SQL 脚本导入的测试数据，这些数据只会存在于 MySQL 数据库中，不会自动同步到 Elasticsearch。
2. 如果 Elasticsearch 服务重启或数据丢失，索引也会变为空。

## 解决方案

我已为您执行了以下修复：
1. **修复代码**：修改了 `backend/search-service/.../SearchServiceImpl.java`，将 `termQuery` 改为 `matchQuery`，解决了状态过滤的大小写问题。
2. **创建修复脚本**：创建了 `scripts/rebuild_index.sh` 用于触发索引重建。

### 操作步骤

请按照以下步骤操作以彻底解决问题：

#### 第一步：重启后端服务 (应用代码修复)
由于修改了 Java 代码，需要重新编译并启动服务。

```bash
# 在项目根目录下运行
bash scripts/start.sh
```
等待所有服务启动完成。

#### 第二步：重建索引 (同步数据)
服务启动后，运行修复脚本将数据库中的知识同步到 Elasticsearch。

```bash
# 在项目根目录下运行
bash scripts/rebuild_index.sh
```

如果脚本执行成功，您将看到 "Rebuild successful" 或类似的成功提示。

### 方法二：手动调用接口

如果您无法运行脚本，可以使用 Postman 或 curl 手动调用接口：

**1. 登录获取 Token**
- URL: `http://localhost:8080/api/auth/login`
- Method: `POST`
- Body:
```json
{
    "username": "admin",
    "password": "password123"
}
```
- 响应中会包含 `token`。

**2. 触发重建索引**
- URL: `http://localhost:8080/api/knowledge/search/reindex`
- Method: `POST`
- Headers:
    - `Authorization`: `您的Token`

## 验证修复
脚本执行成功后，请等待几秒钟（取决于数据量），然后刷新知识库搜索页面，尝试搜索关键词（如"知识"、"测试"等）。

## 注意事项
- 确保 Elasticsearch 服务正在运行 (端口 9200)。
- 确保 Search Service 正在运行 (端口 8083)。
- 重建索引是一个耗时操作，如果数据量很大，请耐心等待。
