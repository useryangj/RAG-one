#!/bin/bash

# RAG-one 项目部署脚本
# 使用方法: ./deploy.sh [环境] [操作]
# 环境: dev|prod
# 操作: build|up|down|restart|logs

set -e

# 默认参数
ENVIRONMENT=${1:-prod}
ACTION=${2:-up}

# 颜色输出
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

# 检查 Docker 和 Docker Compose
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    log_success "Docker 环境检查通过"
}

# 检查环境变量文件
check_env_file() {
    if [ ! -f ".env" ]; then
        if [ -f "env.example" ]; then
            log_warning ".env 文件不存在，正在从 env.example 创建..."
            cp env.example .env
            log_warning "请编辑 .env 文件，设置正确的环境变量"
            exit 1
        else
            log_error ".env 文件和 env.example 文件都不存在"
            exit 1
        fi
    fi
    log_success "环境变量文件检查通过"
}

# 构建镜像
build_images() {
    log_info "开始构建 Docker 镜像..."
    
    # 构建后端镜像
    log_info "构建后端镜像..."
    docker build -f Dockerfile.backend -t ragone-backend:latest .
    
    # 构建前端镜像
    log_info "构建前端镜像..."
    cd frontend
    docker build -t ragone-frontend:latest .
    cd ..
    
    log_success "Docker 镜像构建完成"
}

# 启动服务
start_services() {
    log_info "启动服务..."
    docker-compose --env-file .env up -d
    
    # 等待服务启动
    log_info "等待服务启动..."
    sleep 30
    
    # 检查服务状态
    check_services_health
}

# 停止服务
stop_services() {
    log_info "停止服务..."
    docker-compose down
    log_success "服务已停止"
}

# 重启服务
restart_services() {
    log_info "重启服务..."
    docker-compose restart
    sleep 30
    check_services_health
}

# 查看日志
show_logs() {
    docker-compose logs -f
}

# 检查服务健康状态
check_services_health() {
    log_info "检查服务健康状态..."
    
    # 检查数据库
    if docker-compose exec postgres pg_isready -U ragone_user -d ragone > /dev/null 2>&1; then
        log_success "PostgreSQL 数据库运行正常"
    else
        log_error "PostgreSQL 数据库连接失败"
    fi
    
    # 检查 Redis
    if docker-compose exec redis redis-cli ping > /dev/null 2>&1; then
        log_success "Redis 缓存运行正常"
    else
        log_error "Redis 缓存连接失败"
    fi
    
    # 检查后端服务
    if curl -f http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
        log_success "后端服务运行正常"
    else
        log_warning "后端服务可能还在启动中，请稍后检查"
    fi
    
    # 检查前端服务
    if curl -f http://localhost/health > /dev/null 2>&1; then
        log_success "前端服务运行正常"
    else
        log_warning "前端服务可能还在启动中，请稍后检查"
    fi
}

# 清理资源
cleanup() {
    log_info "清理 Docker 资源..."
    docker-compose down -v --remove-orphans
    docker system prune -f
    log_success "清理完成"
}

# 备份数据
backup_data() {
    log_info "备份数据..."
    
    # 创建备份目录
    BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$BACKUP_DIR"
    
    # 备份数据库
    docker-compose exec postgres pg_dump -U ragone_user ragone > "$BACKUP_DIR/database.sql"
    
    # 备份上传的文件
    docker cp ragone-backend:/app/uploads "$BACKUP_DIR/"
    
    log_success "数据备份完成: $BACKUP_DIR"
}

# 显示帮助信息
show_help() {
    echo "RAG-one 项目部署脚本"
    echo ""
    echo "使用方法:"
    echo "  $0 [环境] [操作]"
    echo ""
    echo "环境:"
    echo "  dev     开发环境"
    echo "  prod    生产环境 (默认)"
    echo ""
    echo "操作:"
    echo "  build     构建 Docker 镜像"
    echo "  up        启动服务 (默认)"
    echo "  down      停止服务"
    echo "  restart   重启服务"
    echo "  logs      查看日志"
    echo "  health    检查服务健康状态"
    echo "  cleanup   清理 Docker 资源"
    echo "  backup    备份数据"
    echo "  help      显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 prod build    # 构建生产环境镜像"
    echo "  $0 prod up       # 启动生产环境服务"
    echo "  $0 prod logs     # 查看生产环境日志"
}

# 主函数
main() {
    case $ACTION in
        "build")
            check_docker
            check_env_file
            build_images
            ;;
        "up")
            check_docker
            check_env_file
            start_services
            ;;
        "down")
            check_docker
            stop_services
            ;;
        "restart")
            check_docker
            restart_services
            ;;
        "logs")
            check_docker
            show_logs
            ;;
        "health")
            check_services_health
            ;;
        "cleanup")
            check_docker
            cleanup
            ;;
        "backup")
            check_docker
            backup_data
            ;;
        "help")
            show_help
            ;;
        *)
            log_error "未知操作: $ACTION"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main

