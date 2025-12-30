#!/bin/bash

# 企业知识库管理系统启动脚本

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
echo "2. 启动服务..."
echo "注意: 请确保以下服务已启动:"
echo "  - MySQL (端口 3306)"
echo "  - Redis (端口 6379)"
echo "  - ElasticSearch (端口 9200)"
echo "  - Nacos (端口 8848)"
echo ""
read -p "按回车键继续启动服务..."

# 确保在 backend 目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 创建 logs 目录
mkdir -p logs

# 启动各个服务（后台运行）
echo "启动 file-service..."
cd file-service && nohup mvn spring-boot:run > ../logs/file-service.log 2>&1 &
FILE_PID=$!
cd "$SCRIPT_DIR"

echo "启动 knowledge-service..."
cd knowledge-service && nohup mvn spring-boot:run > ../logs/knowledge-service.log 2>&1 &
KNOWLEDGE_PID=$!
cd "$SCRIPT_DIR"

echo "启动 search-service..."
cd search-service && nohup mvn spring-boot:run > ../logs/search-service.log 2>&1 &
SEARCH_PID=$!
cd "$SCRIPT_DIR"

echo "启动 audit-service..."
cd audit-service && nohup mvn spring-boot:run > ../logs/audit-service.log 2>&1 &
AUDIT_PID=$!
cd "$SCRIPT_DIR"

echo "启动 user-service..."
cd user-service && nohup mvn spring-boot:run > ../logs/user-service.log 2>&1 &
USER_PID=$!
cd "$SCRIPT_DIR"

echo "启动 gateway-service..."
cd gateway-service && nohup mvn spring-boot:run > ../logs/gateway-service.log 2>&1 &
GATEWAY_PID=$!
cd "$SCRIPT_DIR"

# 保存PID到文件
echo $FILE_PID > logs/file-service.pid
echo $KNOWLEDGE_PID > logs/knowledge-service.pid
echo $SEARCH_PID > logs/search-service.pid
echo $AUDIT_PID > logs/audit-service.pid
echo $USER_PID > logs/user-service.pid
echo $GATEWAY_PID > logs/gateway-service.pid

echo ""
echo "=========================================="
echo "  服务启动完成！"
echo "=========================================="
echo "服务PID已保存到 logs/ 目录"
echo ""
echo "查看日志:"
echo "  tail -f logs/file-service.log"
echo "  tail -f logs/gateway-service.log"
echo ""
echo "停止服务:"
echo "  ./stop.sh"
echo ""

