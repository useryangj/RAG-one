#!/bin/bash

# RAG 智能问答系统开发环境停止脚本

echo "🛑 停止 RAG 智能问答系统开发环境..."

# 读取 PID 文件并停止服务
if [ -f "logs/backend.pid" ]; then
    BACKEND_PID=$(cat logs/backend.pid)
    if ps -p $BACKEND_PID > /dev/null 2>&1; then
        echo "📦 停止后端服务 (PID: $BACKEND_PID)..."
        kill $BACKEND_PID
        sleep 2
        if ps -p $BACKEND_PID > /dev/null 2>&1; then
            echo "   强制停止后端服务..."
            kill -9 $BACKEND_PID
        fi
        echo "✅ 后端服务已停止"
    else
        echo "⚠️  后端服务未运行"
    fi
    rm -f logs/backend.pid
else
    echo "⚠️  未找到后端服务 PID 文件"
fi

if [ -f "logs/frontend.pid" ]; then
    FRONTEND_PID=$(cat logs/frontend.pid)
    if ps -p $FRONTEND_PID > /dev/null 2>&1; then
        echo "🌐 停止前端服务 (PID: $FRONTEND_PID)..."
        kill $FRONTEND_PID
        sleep 2
        if ps -p $FRONTEND_PID > /dev/null 2>&1; then
            echo "   强制停止前端服务..."
            kill -9 $FRONTEND_PID
        fi
        echo "✅ 前端服务已停止"
    else
        echo "⚠️  前端服务未运行"
    fi
    rm -f logs/frontend.pid
else
    echo "⚠️  未找到前端服务 PID 文件"
fi

# 额外清理：根据端口停止服务
echo "🧹 清理可能残留的服务进程..."

# 停止占用 8080 端口的进程（后端）
BACKEND_PORT_PID=$(lsof -ti:8080 2>/dev/null)
if [ ! -z "$BACKEND_PORT_PID" ]; then
    echo "   停止占用 8080 端口的进程: $BACKEND_PORT_PID"
    kill $BACKEND_PORT_PID 2>/dev/null
fi

# 停止占用 3000 端口的进程（前端）
FRONTEND_PORT_PID=$(lsof -ti:3000 2>/dev/null)
if [ ! -z "$FRONTEND_PORT_PID" ]; then
    echo "   停止占用 3000 端口的进程: $FRONTEND_PORT_PID"
    kill $FRONTEND_PORT_PID 2>/dev/null
fi

echo "✅ RAG 智能问答系统开发环境已完全停止"
