#!/bin/bash

# RAG 智能问答系统开发环境启动脚本

echo "🚀 启动 RAG 智能问答系统开发环境..."

# 检查是否安装了必要的工具
if ! command -v java &> /dev/null; then
    echo "❌ Java 未安装，请先安装 Java 17+"
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo "❌ Node.js 未安装，请先安装 Node.js 18+"
    exit 1
fi

if ! command -v npm &> /dev/null; then
    echo "❌ npm 未安装，请先安装 npm"
    exit 1
fi

# 检查 PostgreSQL 是否运行
if ! pg_isready -h localhost -p 5432 &> /dev/null; then
    echo "⚠️  PostgreSQL 未运行，请先启动 PostgreSQL 服务"
    echo "   Ubuntu/Debian: sudo systemctl start postgresql"
    echo "   macOS: brew services start postgresql"
    exit 1
fi

# 创建日志目录
mkdir -p logs

# 启动后端服务
echo "📦 启动 Spring Boot 后端服务..."
cd "$(dirname "$0")"
nohup ./mvnw spring-boot:run > logs/backend.log 2>&1 &
BACKEND_PID=$!
echo "   后端服务 PID: $BACKEND_PID"

# 等待后端服务启动
echo "⏳ 等待后端服务启动..."
for i in {1..30}; do
    if curl -s http://localhost:8080/api/auth/me > /dev/null 2>&1; then
        echo "✅ 后端服务启动成功"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ 后端服务启动超时"
        kill $BACKEND_PID 2>/dev/null
        exit 1
    fi
    sleep 2
done

# 启动前端服务
echo "🌐 启动 React 前端服务..."
cd frontend

# 检查是否已安装依赖
if [ ! -d "node_modules" ]; then
    echo "📥 安装前端依赖..."
    npm install
fi

# 启动前端开发服务器
nohup npm start > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "   前端服务 PID: $FRONTEND_PID"

# 等待前端服务启动
echo "⏳ 等待前端服务启动..."
for i in {1..20}; do
    if curl -s http://localhost:3000 > /dev/null 2>&1; then
        echo "✅ 前端服务启动成功"
        break
    fi
    if [ $i -eq 20 ]; then
        echo "❌ 前端服务启动超时"
        kill $BACKEND_PID $FRONTEND_PID 2>/dev/null
        exit 1
    fi
    sleep 3
done

# 保存 PID 到文件
echo $BACKEND_PID > logs/backend.pid
echo $FRONTEND_PID > logs/frontend.pid

echo ""
echo "🎉 RAG 智能问答系统启动成功！"
echo ""
echo "📍 服务地址："
echo "   前端: http://localhost:3000"
echo "   后端: http://localhost:8080/api"
echo ""
echo "📋 管理命令："
echo "   查看后端日志: tail -f logs/backend.log"
echo "   查看前端日志: tail -f logs/frontend.log"
echo "   停止服务: ./stop-dev.sh"
echo ""
echo "⏹️  按 Ctrl+C 停止服务"

# 等待用户中断
trap 'echo ""; echo "🛑 正在停止服务..."; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; rm -f logs/*.pid; echo "✅ 服务已停止"; exit 0' INT

# 保持脚本运行
while true; do
    sleep 1
done
