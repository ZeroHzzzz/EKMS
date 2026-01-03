#!/bin/bash

# 重新初始化数据库脚本
# 此脚本会删除并重新创建 knowledge_db 数据库，然后执行 init.sql
# 注意：此操作会删除所有数据，请谨慎使用！

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

echo "=========================================="
echo "重新初始化数据库"
echo "=========================================="
echo "警告：此操作将删除所有现有数据！"
read -p "确认继续？(y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "已取消"
    exit 1
fi

# 删除并重新创建数据库
echo "1. 删除旧数据库..."
docker exec knowledge-mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS knowledge_db;" 2>&1 | grep -v "Warning"

echo "2. 创建新数据库..."
docker exec knowledge-mysql mysql -uroot -proot -e "CREATE DATABASE knowledge_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>&1 | grep -v "Warning"

echo "3. 执行初始化脚本..."
docker exec -i knowledge-mysql mysql -uroot -proot knowledge_db < sql/init.sql 2>&1 | grep -v "Warning"

echo "4. 验证数据库结构..."
TABLE_COUNT=$(docker exec knowledge-mysql mysql -uroot -proot knowledge_db -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='knowledge_db';" 2>&1 | grep -v "Warning\|COUNT" | tr -d ' ')

echo ""
echo "已创建 $TABLE_COUNT 个表"
echo "表列表："
docker exec knowledge-mysql mysql -uroot -proot knowledge_db -e "SHOW TABLES;" 2>&1 | grep -v "Warning\|Tables_in" | sed 's/^/  - /'

echo ""
echo "=========================================="
echo "数据库初始化完成！"
echo "=========================================="

