#!/bin/bash

# Redis RAG缓存启动脚本

echo "启动Redis RAG缓存系统..."

# 检查Redis是否已安装
if ! command -v redis-server &> /dev/null; then
    echo "Redis未安装，正在安装Redis..."
    
    # 根据操作系统安装Redis
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Ubuntu/Debian
        sudo apt-get update
        sudo apt-get install -y redis-server
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        if command -v brew &> /dev/null; then
            brew install redis
        else
            echo "请先安装Homebrew或手动安装Redis"
            exit 1
        fi
    else
        echo "不支持的操作系统，请手动安装Redis"
        exit 1
    fi
fi

# 启动Redis服务器
echo "启动Redis服务器..."
redis-server --daemonize yes --port 6379

# 等待Redis启动
sleep 2

# 检查Redis是否正常运行
if redis-cli ping | grep -q "PONG"; then
    echo "✅ Redis启动成功！"
    echo "Redis运行在端口6379"
    echo ""
    echo "现在可以启动RAG应用："
    echo "  ./start-dev.sh"
else
    echo "❌ Redis启动失败，请检查配置"
    exit 1
fi



