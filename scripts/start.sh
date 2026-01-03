#!/bin/bash

# 企业知识库管理系统启动脚本

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT/backend"

echo "=========================================="
echo "  企业知识库管理系统启动脚本"
echo "=========================================="

# 检查是否在WSL环境
if [ -z "$WSL_DISTRO_NAME" ]; then
    echo "警告: 建议在WSL环境中运行"
fi

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java，请先安装JDK 1.8+"
    exit 1
fi

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven，请先安装Maven 3.6+"
    exit 1
fi

echo ""
echo "1. 编译项目..."
mvn clean install -DskipTests

if [ $? -ne 0 ]; then
    echo "编译失败，请检查错误信息"
    exit 1
fi

echo ""
echo "2. 检查基础服务..."
echo "检查以下服务是否运行:"
echo "  - MySQL (端口 3306)"
echo "  - Redis (端口 6379)"
echo "  - ElasticSearch (端口 9200)"
echo "  - Nacos (端口 8848)"
echo ""

# 检查基础服务
MISSING_SERVICES=()

if command -v docker &> /dev/null || command -v sudo &> /dev/null; then
    DOCKER_CMD="docker"
    if ! docker ps &>/dev/null; then
        DOCKER_CMD="sudo docker"
    fi
    
    if ! $DOCKER_CMD ps 2>/dev/null | grep -q "knowledge-mysql"; then
        MISSING_SERVICES+=("MySQL")
    fi
    if ! $DOCKER_CMD ps 2>/dev/null | grep -q "knowledge-redis"; then
        MISSING_SERVICES+=("Redis")
    fi
    if ! $DOCKER_CMD ps 2>/dev/null | grep -q "knowledge-elasticsearch"; then
        MISSING_SERVICES+=("ElasticSearch")
    fi
    if ! $DOCKER_CMD ps 2>/dev/null | grep -q "knowledge-nacos"; then
        MISSING_SERVICES+=("Nacos")
    fi
else
    echo "⚠ 未找到 Docker，无法检查服务状态"
fi

if [ ${#MISSING_SERVICES[@]} -gt 0 ]; then
    echo "⚠ 以下服务未运行: ${MISSING_SERVICES[*]}"
    echo "提示: 请先运行项目根目录的 scripts/init.sh 或 sudo docker compose up -d 启动基础服务"
    read -p "是否继续启动后端服务？(y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "已取消"
        exit 0
    fi
else
    echo "✓ 基础服务检查通过"
fi

echo ""
echo "3. 启动后端服务..."

# 确保在 backend 目录
cd "$PROJECT_ROOT/backend"

# 创建 logs 目录
mkdir -p logs

# 启动各个服务（后台运行）
echo "启动 file-service..."
cd file-service && nohup mvn spring-boot:run > ../logs/file-service.log 2>&1 &
FILE_PID=$!
cd "$PROJECT_ROOT/backend"

echo "启动 knowledge-service..."
cd knowledge-service && nohup mvn spring-boot:run > ../logs/knowledge-service.log 2>&1 &
KNOWLEDGE_PID=$!
cd "$PROJECT_ROOT/backend"

echo "启动 search-service..."
cd search-service && nohup mvn spring-boot:run > ../logs/search-service.log 2>&1 &
SEARCH_PID=$!
cd "$PROJECT_ROOT/backend"

echo "启动 audit-service..."
cd audit-service && nohup mvn spring-boot:run > ../logs/audit-service.log 2>&1 &
AUDIT_PID=$!
cd "$PROJECT_ROOT/backend"

echo "启动 user-service..."
cd user-service && nohup mvn spring-boot:run > ../logs/user-service.log 2>&1 &
USER_PID=$!
cd "$PROJECT_ROOT/backend"

echo "启动 gateway-service..."
cd gateway-service && nohup mvn spring-boot:run > ../logs/gateway-service.log 2>&1 &
GATEWAY_PID=$!
cd "$PROJECT_ROOT/backend"

# 保存PID到文件
echo $FILE_PID > logs/file-service.pid
echo $KNOWLEDGE_PID > logs/knowledge-service.pid
echo $SEARCH_PID > logs/search-service.pid
echo $AUDIT_PID > logs/audit-service.pid
echo $USER_PID > logs/user-service.pid
echo $GATEWAY_PID > logs/gateway-service.pid

echo ""
echo "等待服务启动..."
sleep 5

echo ""
echo "=========================================="
echo "  服务启动完成！"
echo "=========================================="
echo "服务PID已保存到 backend/logs/ 目录"
echo ""
echo "服务端口："
echo "  - Gateway: http://localhost:8080"
echo "  - File Service: 8081"
echo "  - Knowledge Service: 8082"
echo "  - Search Service: 8083"
echo "  - Audit Service: 8084"
echo "  - User Service: 8085"
echo ""
echo "查看日志:"
echo "  tail -f backend/logs/gateway-service.log"
echo "  tail -f backend/logs/knowledge-service.log"
echo ""
echo "停止服务:"
echo "  scripts/stop.sh"
echo ""
