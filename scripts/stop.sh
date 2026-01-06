#!/bin/bash

# 停止所有后端服务

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT/backend"

echo "=========================================="
echo "  停止所有后端服务"
echo "=========================================="

STOPPED_COUNT=0

# 通过PID文件停止服务
if [ -d "logs" ]; then
    for pidfile in logs/*.pid; do
        if [ -f "$pidfile" ]; then
            PID=$(cat "$pidfile")
            SERVICE_NAME=$(basename "$pidfile" .pid)
            if ps -p $PID > /dev/null 2>&1; then
                echo "停止 $SERVICE_NAME (PID: $PID)..."
                kill $PID 2>/dev/null || true
                STOPPED_COUNT=$((STOPPED_COUNT + 1))
            fi
            rm "$pidfile"
        fi
    done
fi

# 查找并停止所有 java -jar 进程 (作为后备方案)
SPRING_PIDS=$(pgrep -f "java.*-jar.*-service.*.jar" 2>/dev/null || true)
if [ -n "$SPRING_PIDS" ]; then
    echo "停止残留的 Java 服务进程..."
    echo "$SPRING_PIDS" | xargs kill 2>/dev/null || true
    STOPPED_COUNT=$((STOPPED_COUNT + $(echo "$SPRING_PIDS" | wc -l)))
fi

# 等待进程完全退出
if [ $STOPPED_COUNT -gt 0 ]; then
    echo "等待进程退出..."
    sleep 2
fi

echo ""
if [ $STOPPED_COUNT -gt 0 ]; then
    echo "✓ 已停止 $STOPPED_COUNT 个服务"
else
    echo "✓ 没有运行中的服务"
fi
echo ""
