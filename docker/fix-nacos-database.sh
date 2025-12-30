#!/bin/bash

# 修复 Nacos 数据库缺失表的问题
# 此脚本用于重新初始化 Nacos 数据库表

echo "=========================================="
echo "Nacos 数据库表修复脚本"
echo "=========================================="

# 检查 MySQL 是否运行
echo "检查 MySQL 容器状态..."
sudo docker compose ps mysql | grep -q "Up"
if [ $? -ne 0 ]; then
    echo "启动 MySQL 容器..."
    sudo docker compose up -d mysql
    echo "等待 MySQL 启动..."
    sleep 15
fi

# 删除并重新创建数据库（可选，会清除数据）
read -p "是否删除并重新创建 nacos_config 数据库? 这将清除所有数据 (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "删除 nacos_config 数据库..."
    sudo docker compose exec -T mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS nacos_config;"
    
    echo "创建 nacos_config 数据库..."
    sudo docker compose exec -T mysql mysql -uroot -proot -e "CREATE DATABASE nacos_config CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    
    echo "执行初始化脚本..."
    sudo docker compose exec -T mysql mysql -uroot -proot nacos_config < sql/mysql-schema.sql
    
    echo "✓ 数据库已重新初始化"
else
    # 只创建缺失的表
    echo "检查并创建缺失的表..."
    
    # 检查 config_info_aggr 表是否存在
    TABLE_EXISTS=$(sudo docker compose exec -T mysql mysql -uroot -proot nacos_config -e "SHOW TABLES LIKE 'config_info_aggr';" | grep config_info_aggr)
    if [ -z "$TABLE_EXISTS" ]; then
        echo "创建 config_info_aggr 表..."
        sudo docker compose exec -T mysql mysql -uroot -proot nacos_config <<EOF
CREATE TABLE IF NOT EXISTS \`config_info_aggr\` (
    \`id\` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    \`data_id\` varchar(255) NOT NULL COMMENT 'data_id',
    \`group_id\` varchar(128) NOT NULL COMMENT 'group_id',
    \`datum_id\` varchar(255) NOT NULL COMMENT 'datum_id',
    \`content\` longtext NOT NULL COMMENT 'content',
    \`gmt_modified\` datetime NOT NULL COMMENT '修改时间',
    \`app_name\` varchar(128) DEFAULT NULL COMMENT 'app_name',
    \`tenant_id\` varchar(128) DEFAULT '' COMMENT '租户字段',
    PRIMARY KEY (\`id\`),
    UNIQUE KEY \`uk_configinfoaggr_datagrouptenantdatum\` (\`data_id\`,\`group_id\`,\`tenant_id\`,\`datum_id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='增加租户字段';
EOF
        echo "✓ config_info_aggr 表已创建"
    else
        echo "✓ config_info_aggr 表已存在"
    fi
fi

echo ""
echo "数据库修复完成！"
echo "现在可以重启 Nacos 容器："
echo "  sudo docker compose restart nacos"
echo "  sudo docker compose logs -f nacos"

