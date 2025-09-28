#!/bin/bash

# ========================================
# RAG-one 快速部署脚本 (简化版)
# ========================================
# 功能：快速部署RAG-one项目
# 用法：./deploy-quick.sh
# ========================================

set -e

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}[INFO]${NC} 开始快速部署 RAG-one 项目..."

# 检查Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装"
    exit 1
fi

# 检查必要文件
if [[ ! -f "docker-compose.fullstack.yml" ]]; then
    echo "❌ 缺少 docker-compose.fullstack.yml 文件"
    exit 1
fi

# 创建.env文件（如果不存在）
if [[ ! -f ".env" ]]; then
    echo -e "${BLUE}[INFO]${NC} 创建 .env 文件..."
    cat > .env << 'EOF'
# 数据库配置
DB_PASSWORD=ragone_password
DB_USERNAME=ragone_user
DB_NAME=ragone
DB_HOST=postgres
DB_PORT=5432

# Redis 配置
REDIS_PASSWORD=
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_DATABASE=0

# JWT 配置（必须至少32位）
JWT_SECRET=mySecretKey123456789012345678901234567890

# AI 模型配置
SILICONFLOW_API_KEY=sk-lccjubwiwtvrfbtubsuqclecnhzalcbckwbpdrielsonkcgw
LLM_MODEL=Qwen/Qwen3-Next-80B-A3B-Instruct
EMBEDDING_MODEL=BAAI/bge-large-zh-v1.5

# 文件存储
FILE_STORAGE_PATH=/app/uploads

# 前端配置
REACT_APP_API_BASE_URL=http://localhost:8080/api

# 服务器配置
SERVER_PORT=8080

# 文档处理配置
DOCUMENT_CHUNK_MAX_CHARS=100
DOCUMENT_CHUNK_OVERLAP=50
DOCUMENT_CHUNK_MAX_TOKENS=500
EOF
    echo -e "${BLUE}[INFO]${NC} 请编辑 .env 文件设置正确的 API 密钥"
fi

# 停止现有服务
echo -e "${BLUE}[INFO]${NC} 停止现有服务..."
docker compose -f docker-compose.fullstack.yml down 2>/dev/null || true

# 构建并启动
echo -e "${BLUE}[INFO]${NC} 构建并启动服务..."
docker compose -f docker-compose.fullstack.yml up -d --build

# 等待服务启动
echo -e "${BLUE}[INFO]${NC} 等待服务启动..."
sleep 30

# 初始化数据库
echo -e "${BLUE}[INFO]${NC} 初始化数据库..."
if [[ -f "data/ragone_schema.sql" ]]; then
    docker exec -i ragone-postgres psql -U ragone_user -d ragone < data/ragone_schema.sql 2>/dev/null || true
fi

# 检查服务状态
echo -e "${BLUE}[INFO]${NC} 检查服务状态..."
docker compose -f docker-compose.fullstack.yml ps

echo ""
echo -e "${GREEN}🎉 部署完成！${NC}"
echo ""
echo "🌐 前端访问地址: http://localhost:8080"
echo "🔧 后端API地址: http://localhost:8080/api"
echo ""
echo "📋 常用命令:"
echo "   - 查看日志: docker logs ragone-app"
echo "   - 重启服务: docker compose -f docker-compose.fullstack.yml restart"
echo "   - 停止服务: docker compose -f docker-compose.fullstack.yml down"
echo ""
