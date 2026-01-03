# 脚本使用说明

所有脚本统一放在 `scripts/` 目录下。

## 核心脚本

### 1. `scripts/init.sh` - 初始化脚本
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

**注意：**
- 会删除所有数据库数据
- 会清理所有日志和上传文件
- 需要确认后才会执行

---

### 2. `scripts/start.sh` - 启动后端服务
**用途：** 编译并启动所有后端微服务

**功能：**
- 检查 Java 和 Maven 环境
- 编译项目
- 检查基础服务（MySQL、Redis、ElasticSearch、Nacos）
- 启动所有微服务（后台运行）
- 保存服务 PID 到 logs 目录

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

### 3. `scripts/stop.sh` - 停止后端服务
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

### 仅重置数据库
```bash
# 使用数据库重新初始化脚本
cd sql
./reinit_database.sh
```

---

## 脚本位置

```
EKMS/
├── scripts/                   # 所有脚本目录
│   ├── init.sh               # 初始化脚本
│   ├── start.sh              # 启动脚本
│   ├── stop.sh               # 停止脚本
│   └── README.md             # 脚本说明文档
└── sql/
    └── reinit_database.sh    # 数据库重新初始化脚本
```

---

## 注意事项

1. **初始化脚本**会删除所有数据，请谨慎使用
2. **启动脚本**需要先确保基础服务（Docker）已运行
3. **停止脚本**会强制停止所有 Spring Boot 进程
4. 所有脚本都需要执行权限：`chmod +x script.sh`

---

## 故障排查

### 服务启动失败
1. 检查基础服务是否运行：`docker-compose ps`
2. 查看日志：`tail -f backend/logs/*.log`
3. 检查端口占用：`netstat -tlnp | grep 808`

### 数据库连接失败
1. 检查 MySQL 容器：`docker ps | grep mysql`
2. 检查数据库：`docker exec knowledge-mysql mysql -uroot -proot -e "SHOW DATABASES;"`
3. 重新初始化：`./init.sh` 或 `sql/reinit_database.sh`

### 端口被占用
1. 查找占用进程：`lsof -i :8080`
2. 停止占用进程或修改配置文件中的端口

