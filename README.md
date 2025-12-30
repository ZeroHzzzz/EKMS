# 企业知识库管理系统

基于 SpringBoot + Dubbo 微服务架构的企业知识库管理系统。

## 技术栈

### 后端
- Spring Boot 2.7.18
- Apache Dubbo 3.2.0（RPC框架）
- MyBatis Plus 3.5.3.1
- MySQL 8.0
- ElasticSearch 7.17.9
- Redis
- Nacos 2.2.3（服务注册与发现）

### 前端
- Vue 3
- Element Plus
- Vite
- Axios

## 项目架构

```
┌─────────────────┐
│ gateway-service │  (API 网关，端口 8080)
└────────┬────────┘
         │ Dubbo RPC 调用
         ├─────────────┬──────────────┬─────────────┬─────────────┐
         │             │              │             │             │
    ┌────▼────┐  ┌────▼────┐  ┌─────▼─────┐  ┌───▼────┐  ┌─────▼─────┐
    │  user-  │  │  file-  │  │knowledge- │  │search- │  │  audit-   │
    │ service │  │ service │  │  service  │  │service │  │  service  │
    │  8085   │  │  8081   │  │   8082    │  │  8083  │  │   8084    │
    └─────────┘  └─────────┘  └───────────┘  └────────┘  └───────────┘
         │             │              │             │             │
         └─────────────┴──────────────┴─────────────┴─────────────┘
                                    │
                           ┌────────▼────────┐
                           │  Nacos Registry │
                           │   (端口 8848)   │
                           └─────────────────┘
```

## 快速开始

### 第一步：启动基础服务（Docker）

```bash
# 在项目根目录执行（WSL 环境）
cd /mnt/c/Users/ZeroHzzzz/Desktop/java

# 启动所有基础服务（MySQL、Redis、ElasticSearch、Nacos）
sudo docker compose up -d

# 等待服务启动（约 30-60 秒）
# 验证 Nacos：访问 http://localhost:8848/nacos (用户名/密码: nacos/nacos)
```

**注意**：数据库会在首次启动时自动初始化（通过 `docker-compose.yml` 配置）。

### 第二步：启动微服务

#### 方式一：使用启动脚本（推荐）

```bash
# 进入后端目录
cd backend

# 设置 Docker 环境变量
export SPRING_PROFILES_ACTIVE=docker

# 使用启动脚本（会自动编译并启动所有服务）
bash start.sh
```

#### 方式二：手动启动

```bash
# 1. 编译项目
cd backend
mvn clean install -DskipTests

# 2. 设置环境变量
export SPRING_PROFILES_ACTIVE=docker

# 3. 按顺序启动各个服务（每个服务需要独立的终端）
cd user-service && mvn spring-boot:run
cd file-service && mvn spring-boot:run
cd knowledge-service && mvn spring-boot:run
cd search-service && mvn spring-boot:run
cd audit-service && mvn spring-boot:run
cd gateway-service && mvn spring-boot:run  # 最后启动
```

**启动顺序很重要**：gateway-service 必须最后启动，因为它依赖其他服务。

### 第三步：启动前端

```bash
cd frontend
npm install
npm run dev
```

访问：http://localhost:3000

## 服务端口

| 服务 | HTTP 端口 | Dubbo 端口 | 说明 |
|------|----------|------------|------|
| gateway-service | 8080 | 20880 | API 网关 |
| file-service | 8081 | 20881 | 文件服务 |
| knowledge-service | 8082 | 20882 | 知识服务 |
| search-service | 8083 | 20883 | 搜索服务 |
| audit-service | 8084 | 20884 | 审核服务 |
| user-service | 8085 | 20885 | 用户服务 |

## 验证服务

### 1. 检查 Nacos 服务注册

访问：http://localhost:8848/nacos
- 在"服务管理" → "服务列表"中应该能看到所有 6 个服务

### 2. 测试 API

```bash
# 测试网关服务
curl http://localhost:8080/api/knowledge/list
```

### 3. 查看日志

```bash
# 如果使用启动脚本，日志在 logs/ 目录
tail -f backend/logs/gateway-service.log
```

## 停止服务

```bash
# 停止微服务
cd backend
bash stop.sh

# 停止基础服务（Docker）
sudo docker compose down
```

## 常见问题

### Nacos 启动失败

如果 Nacos 出现数据库相关错误，使用修复脚本：

```bash
bash docker/fix-nacos-database.sh
```

### 服务无法注册到 Nacos

- 检查 Nacos 是否正常运行：`curl http://localhost:8848/nacos/`
- 检查环境变量：`echo $SPRING_PROFILES_ACTIVE`（应该是 `docker`）
- 查看服务日志

### 端口被占用

```bash
# 查看端口占用
sudo netstat -tulpn | grep 8080
# 停止占用端口的进程
sudo kill <PID>
```

## 详细文档

- [部署指南](./docs/部署指南.md) - 完整的部署说明和常见问题

## 项目结构

```
java/
├── backend/              # 后端微服务
│   ├── gateway-service/  # API 网关
│   ├── user-service/     # 用户服务
│   ├── file-service/     # 文件服务
│   ├── knowledge-service/# 知识服务
│   ├── search-service/   # 搜索服务
│   ├── audit-service/    # 审核服务
│   ├── knowledge-api/    # API 接口定义
│   ├── knowledge-common/ # 公共模块
│   ├── start.sh          # 启动脚本
│   └── stop.sh           # 停止脚本
│
├── frontend/             # 前端代码
├── docker/               # Docker 相关脚本
├── sql/                  # 数据库脚本
├── docs/                 # 项目文档
└── docker-compose.yml    # Docker Compose 配置
```

## 许可证

MIT License
