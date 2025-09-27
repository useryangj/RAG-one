#!/bin/bash

# RAG-one 部署脚本
# 使用方法: ./deploy.sh [dev|prod]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查环境
check_environment() {
    log_info "检查部署环境..."
    
    # 检查 Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    # 检查 Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    # 检查环境文件
    if [ ! -f "env.production" ]; then
        log_warning "未找到 env.production 文件，将使用默认配置"
        cp env.example env.production
    fi
    
    log_success "环境检查完成"
}

# 构建镜像
build_images() {
    log_info "构建 Docker 镜像..."
    
    # 构建后端镜像
    log_info "构建后端镜像..."
    docker build -f Dockerfile.backend -t ragone-backend:latest .
    
    # 构建前端镜像
    log_info "构建前端镜像..."
    docker build -f frontend/Dockerfile -t ragone-frontend:latest ./frontend
    
    log_success "镜像构建完成"
}

# 启动服务
start_services() {
    local env=$1
    
    log_info "启动服务 (环境: $env)..."
    
    if [ "$env" = "prod" ]; then
        # 生产环境
        docker-compose --env-file env.production up -d
    else
        # 开发环境
        docker-compose up -d
    fi
    
    log_success "服务启动完成"
}

# 等待服务就绪
wait_for_services() {
    log_info "等待服务就绪..."
    
    # 等待数据库
    log_info "等待 PostgreSQL 启动..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if docker-compose exec -T postgres pg_isready -U ragone_user -d ragone &> /dev/null; then
            break
        fi
        sleep 2
        timeout=$((timeout - 2))
    done
    
    if [ $timeout -le 0 ]; then
        log_error "PostgreSQL 启动超时"
        exit 1
    fi
    
    # 等待 Redis
    log_info "等待 Redis 启动..."
    timeout=30
    while [ $timeout -gt 0 ]; do
        if docker-compose exec -T redis redis-cli ping &> /dev/null; then
            break
        fi
        sleep 2
        timeout=$((timeout - 2))
    done
    
    if [ $timeout -le 0 ]; then
        log_error "Redis 启动超时"
        exit 1
    fi
    
    # 等待后端服务
    log_info "等待后端服务启动..."
    timeout=120
    while [ $timeout -gt 0 ]; do
        if curl -f http://localhost:8080/actuator/health &> /dev/null; then
            break
        fi
        sleep 5
        timeout=$((timeout - 5))
    done
    
    if [ $timeout -le 0 ]; then
        log_error "后端服务启动超时"
        exit 1
    fi
    
    # 等待前端服务
    log_info "等待前端服务启动..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if curl -f http://localhost/health &> /dev/null; then
            break
        fi
        sleep 5
        timeout=$((timeout - 5))
    done
    
    if [ $timeout -le 0 ]; then
        log_error "前端服务启动超时"
        exit 1
    fi
    
    log_success "所有服务已就绪"
}

# 显示服务状态
show_status() {
    log_info "服务状态:"
    docker-compose ps
    
    echo ""
    log_info "访问地址:"
    echo "  前端: http://localhost"
    echo "  后端 API: http://localhost:8080/api"
    echo "  健康检查: http://localhost:8080/actuator/health"
    
    echo ""
    log_info "日志查看:"
    echo "  所有服务: docker-compose logs -f"
    echo "  后端服务: docker-compose logs -f backend"
    echo "  前端服务: docker-compose logs -f frontend"
    echo "  数据库: docker-compose logs -f postgres"
    echo "  Redis: docker-compose logs -f redis"
}

# 停止服务
stop_services() {
    log_info "停止服务..."
    docker-compose down
    log_success "服务已停止"
}

# 清理资源
cleanup() {
    log_info "清理资源..."
    docker-compose down -v --remove-orphans
    docker system prune -f
    log_success "资源清理完成"
}

# 主函数
main() {
    local env=${1:-dev}
    
    case $env in
        "dev"|"development")
            log_info "开始开发环境部署..."
            check_environment
            build_images
            start_services "dev"
            wait_for_services
            show_status
            ;;
        "prod"|"production")
            log_info "开始生产环境部署..."
            check_environment
            build_images
            start_services "prod"
            wait_for_services
            show_status
            ;;
        "stop")
            stop_services
            ;;
        "cleanup")
            cleanup
            ;;
        *)
            echo "使用方法: $0 [dev|prod|stop|cleanup]"
            echo "  dev     - 开发环境部署"
            echo "  prod    - 生产环境部署"
            echo "  stop    - 停止服务"
            echo "  cleanup - 清理资源"
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
