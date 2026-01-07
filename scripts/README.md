# 脚本使用说明

所有管理脚本统一放在 `scripts/` 目录下。

## 脚本列表

### 1. `init.sh` - 系统初始化脚本
**用途：** 将系统恢复到初始状态

**功能：**
- 停止所有后端服务
- 停止 Docker 服务
- 清理并重新初始化数据库
- 清理日志文件
- 清理上传文件（可选）
- 重新启动基础服务（Docker）

**使用方法：**
```bash
cd /mnt/c/Users/31903/Desktop/EKMS
./scripts/init.sh
```

---

### 2. `start.sh` - 启动后端服务
**用途：** 编译并启动所有后端微服务

**功能：**
- 检查 Java 和 Maven 环境
- 编译项目
- 检查基础服务状态
- 启动所有微服务（后台运行）
- 保存服务 PID

**使用方法：**
```bash
cd /mnt/c/Users/31903/Desktop/EKMS
./scripts/start.sh
```

**启动的服务：**
- Gateway Service (8080)
- File Service (8081)
- Knowledge Service (8082)
- Search Service (8083)
- Audit Service (8084)
- User Service (8085)

---

### 3. `stop.sh` - 停止后端服务
**用途：** 停止所有运行中的后端服务

**功能：**
- 通过 PID 文件停止服务
- 查找并停止所有 Spring Boot 进程
- 清理 PID 文件

**使用方法：**
```bash
cd /mnt/c/Users/31903/Desktop/EKMS
./scripts/stop.sh
```

---

## 使用流程

### 首次部署或完全重置
```bash
# 1. 初始化系统（清理所有数据，重新初始化）
./scripts/init.sh

# 2. 启动后端服务
./scripts/start.sh

# 3. 启动前端（如需要）
cd frontend
npm install
npm run dev
```

### 日常开发
```bash
# 1. 启动基础服务（如果未运行）
sudo docker compose up -d

# 2. 启动后端服务
./scripts/start.sh

# 3. 停止服务（开发完成后）
./scripts/stop.sh
```

---

## 注意事项

1. **初始化脚本**会删除所有数据，请谨慎使用
2. **启动脚本**需要先确保基础服务（Docker）已运行
3. **停止脚本**会强制停止所有 Spring Boot 进程
4. 所有脚本都需要执行权限（已设置）

---

## 故障排查

### 服务启动失败
1. 检查基础服务：`sudo docker compose ps`
2. 查看日志：`tail -f backend/logs/*.log`
3. 检查端口占用：`netstat -tlnp | grep 808`

### 数据库连接失败
1. 检查 MySQL 容器：`sudo docker ps | grep mysql`
2. 重新初始化：`./scripts/init.sh`

---

## 知识库搜索问题修复

如果知识库搜索页面无法搜索到任何内容，通常是因为 Elasticsearch 索引为空或与数据库不同步。

### 修复步骤

#### 方法一：使用修复脚本（推荐）

1. **重启后端服务**（确保应用代码是最新的）：
   ```bash
   ./scripts/start.sh
   ```

2. **重建索引**：
   运行修复脚本将数据库中的知识同步到 Elasticsearch。
   ```bash
   ./scripts/rebuild_index.sh
   ```
   如果脚本执行成功，您将看到 "Rebuild successful" 提示。

#### 方法二：手动调用接口

如果您无法运行脚本，可以使用 Postman 或 curl 手动调用接口：

1. **登录获取 Token**
   - URL: `http://localhost:8080/api/auth/login`
   - Method: `POST`
   - Body: `{"username": "admin", "password": "password123"}`

2. **触发重建索引**
   - URL: `http://localhost:8080/api/knowledge/search/reindex`
   - Method: `POST`
   - Headers: `Authorization: 您的Token`

### 验证修复
脚本执行成功后，等待几秒钟，刷新知识库搜索页面，尝试搜索关键词。
