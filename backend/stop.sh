#!/bin/bash

# 停止所有服务

echo "正在停止所有服务..."

if [ -d "logs" ]; then
    for pidfile in logs/*.pid; do
        if [ -f "$pidfile" ]; then
            PID=$(cat "$pidfile")
            if ps -p $PID > /dev/null 2>&1; then
                echo "停止进程 $PID ($(basename $pidfile .pid))"
                kill $PID
            fi
            rm "$pidfile"
        fi
    done
fi

# 查找并停止所有Spring Boot进程
pkill -f "spring-boot:run"

echo "所有服务已停止"

