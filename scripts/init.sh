#!/bin/bash

# 企业知识库管理系统 - 初始化脚本
# 此脚本会将系统恢复到初始状态：
# 1. 停止所有服务
# 2. 清理数据库（重新初始化）
# 3. 清理日志和临时文件
# 4. 清理上传的文件
# 5. 重新启动基础服务

set -e  # 遇到错误立即退出

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  企业知识库管理系统 - 初始化脚本"
echo "=========================================="
echo ""
echo "警告：此操作将："
echo "  - 停止所有运行中的服务"
echo "  - 删除所有数据库数据（重新初始化）"
echo "  - 清理所有日志文件"
echo "  - 清理所有上传的文件"
echo ""
read -p "确认继续？(y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "已取消"
    exit 0
fi

echo ""
echo "=========================================="
echo "步骤 1/6: 停止所有后端服务"
echo "=========================================="
if [ -f "backend/stop.sh" ]; then
    cd backend
    bash stop.sh
    cd ..
    echo "✓ 后端服务已停止"
else
    echo "⚠ 未找到 backend/stop.sh，跳过"
fi

echo ""
echo "=========================================="
echo "步骤 2/6: 停止 Docker 服务"
echo "=========================================="
if command -v sudo docker &> /dev/null; then
    if echo "admin" | sudo -S docker ps | grep -q "knowledge-"; then
        echo "停止 Docker 容器..."
        echo "admin" | sudo -S docker compose down 2>/dev/null || echo "⚠ Docker Compose 停止失败，尝试直接停止容器"
        # 确保容器已停止
        echo "admin" | sudo -S docker stop knowledge-mysql knowledge-redis knowledge-elasticsearch knowledge-nacos 2>/dev/null || true
        echo "✓ Docker 服务已停止"
    else
        echo "✓ 没有运行中的 Docker 容器"
    fi
else
    echo "⚠ 未找到 Docker，跳过"
fi

echo ""
echo "=========================================="
echo "步骤 3/6: 清理数据库（重新初始化）"
echo "=========================================="
if command -v sudo docker &> /dev/null; then
    # 停止并删除 MySQL 容器和数据卷
    echo "停止并删除 MySQL 容器..."
    echo "admin" | sudo -S docker stop knowledge-mysql 2>/dev/null || true
    echo "admin" | sudo -S docker rm -f knowledge-mysql 2>/dev/null || true
    echo "admin" | sudo -S docker volume rm knowledge_mysql_data 2>/dev/null || true
    echo "✓ MySQL 容器和数据卷已删除"
    
    # 重新创建并启动 MySQL 容器
    echo "重新创建 MySQL 容器..."
    cd "$SCRIPT_DIR"
    echo "admin" | sudo -S docker compose up -d mysql 2>/dev/null || {
        echo "⚠ Docker Compose 启动失败，尝试使用 docker run..."
        echo "admin" | sudo -S docker run -d \
            --name knowledge-mysql \
            -e MYSQL_ROOT_PASSWORD=root \
            -e MYSQL_DATABASE=knowledge_db \
            -p 3306:3306 \
            -v knowledge_mysql_data:/var/lib/mysql \
            mysql:8.0 \
            --character-set-server=utf8mb4 \
            --collation-server=utf8mb4_unicode_ci 2>/dev/null || echo "⚠ 无法创建 MySQL 容器"
    }
    
    # 等待 MySQL 启动
    echo "等待 MySQL 启动..."
    sleep 5
    for i in {1..30}; do
        if echo "admin" | sudo -S docker exec knowledge-mysql mysqladmin ping -h localhost -uroot -proot &>/dev/null 2>&1; then
            echo "✓ MySQL 已启动"
            break
        fi
        if [ $i -eq 30 ]; then
            echo "⚠ MySQL 启动超时，但继续执行..."
        else
            sleep 1
        fi
    done
    
    # 重新初始化数据库
    echo "重新初始化数据库..."
    echo "admin" | sudo -S docker exec knowledge-mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS knowledge_db;" 2>&1 | grep -v "Warning" || true
    echo "admin" | sudo -S docker exec knowledge-mysql mysql -uroot -proot -e "CREATE DATABASE knowledge_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>&1 | grep -v "Warning" || true
    
    # 执行初始化SQL脚本
    if [ -f "sql/init.sql" ]; then
        echo "admin" | sudo -S docker exec -i knowledge-mysql mysql -uroot -proot knowledge_db < sql/init.sql 2>&1 | grep -v "Warning" || true
        echo "✓ 已执行 init.sql"
    else
        echo "⚠ 未找到 sql/init.sql 文件"
    fi
    
    # 验证数据库初始化
    TABLE_COUNT=$(echo "admin" | sudo -S docker exec knowledge-mysql mysql -uroot -proot knowledge_db -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='knowledge_db';" 2>&1 | grep -v "Warning\|COUNT" | tr -d ' ' || echo "0")
    if [ "$TABLE_COUNT" -gt "0" ]; then
        echo "✓ 数据库已重新初始化（$TABLE_COUNT 个表）"
    else
        echo "⚠ 数据库初始化可能失败，请检查"
    fi
else
    echo "⚠ 未找到 Docker，跳过数据库初始化"
fi

echo ""
echo "=========================================="
echo "步骤 4/6: 清理日志文件"
echo "=========================================="
if [ -d "backend/logs" ]; then
    rm -f backend/logs/*.log backend/logs/*.pid 2>/dev/null || true
    echo "✓ 日志文件已清理"
else
    mkdir -p backend/logs
    echo "✓ 创建 logs 目录"
fi

echo ""
echo "=========================================="
echo "步骤 5/6: 清理上传的文件"
echo "=========================================="
if [ -d "uploads" ]; then
    read -p "是否删除所有上传的文件？(y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        rm -rf uploads/* 2>/dev/null || true
        echo "✓ 上传文件已清理"
    else
        echo "⚠ 保留上传文件"
    fi
else
    mkdir -p uploads/files uploads/chunks
    echo "✓ 创建 uploads 目录"
fi

echo ""
echo "=========================================="
echo "步骤 6/6: 启动基础服务（Docker）"
echo "=========================================="
if command -v sudo docker &> /dev/null; then
    echo "启动 Docker 服务..."
    cd "$SCRIPT_DIR"
    echo "admin" | sudo -S docker compose up -d 2>/dev/null || {
        echo "⚠ Docker Compose 启动失败，尝试手动启动..."
        echo "admin" | sudo -S docker start knowledge-redis knowledge-elasticsearch knowledge-nacos 2>/dev/null || true
    }
    
    echo "等待服务启动..."
    sleep 10
    
    # 检查服务状态
    echo ""
    echo "服务状态："
    echo "admin" | sudo -S docker compose ps 2>/dev/null || echo "admin" | sudo -S docker ps --filter "name=knowledge-" --format "table {{.Names}}\t{{.Status}}" 2>/dev/null || echo "⚠ 无法获取服务状态"
    
    echo ""
    echo "✓ 基础服务启动完成"
    echo ""
    echo "服务访问地址："
    echo "  - Nacos: http://localhost:8848/nacos (用户名/密码: nacos/nacos)"
    echo "  - ElasticSearch: http://localhost:9200"
    echo "  - MySQL: localhost:3306 (用户名/密码: root/root)"
    echo "  - Redis: localhost:6379"
else
    echo "⚠ 未找到 Docker，跳过基础服务启动"
    echo "  提示：请手动启动 MySQL、Redis、ElasticSearch、Nacos"
fi

echo ""
echo "=========================================="
echo "  初始化完成！"
echo "=========================================="
echo ""
echo "下一步操作："
echo "  1. 检查基础服务是否正常运行"
echo "  2. 运行 backend/start.sh 启动后端服务"
echo "  3. 运行前端开发服务器（如需要）"
echo ""
echo "查看日志："
echo "  sudo docker compose logs -f"
echo "  tail -f backend/logs/*.log"
echo ""

