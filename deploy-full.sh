#!/bin/bash

# ========================================
# RAG-one 完整项目部署脚本
# ========================================
# 功能：一键部署前端、后端、数据库及初始化
# 作者：AI Assistant
# 版本：1.0
# ========================================

set -e  # 遇到错误立即退出

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

# 检查系统要求
check_requirements() {
    log_info "检查系统要求..."
    
    # 检查Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    # 检查Docker Compose
    if ! docker compose version &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    # 检查Docker服务状态
    if ! docker info &> /dev/null; then
        log_error "Docker 服务未运行，请启动 Docker 服务"
        exit 1
    fi
    
    log_success "系统要求检查完成"
}

# 检查必要文件
check_files() {
    log_info "检查必要文件..."
    
    local required_files=(
        "Dockerfile.fullstack"
        "docker-compose.fullstack.yml"
        "data/ragone_schema.sql"
        ".env"
    )
    
    for file in "${required_files[@]}"; do
        if [[ ! -f "$file" ]]; then
            log_error "缺少必要文件: $file"
            exit 1
        fi
    done
    
    log_success "文件检查完成"
}

# 创建环境配置文件
setup_environment() {
    log_info "设置环境配置..."
    
    if [[ ! -f ".env" ]]; then
        log_warning ".env 文件不存在，从 env.example 创建..."
        if [[ -f "env.example" ]]; then
            cp env.example .env
            log_warning "请编辑 .env 文件设置正确的配置"
        else
            log_error "env.example 文件不存在"
            exit 1
        fi
    fi
    
    # 检查关键环境变量
    source .env
    
    if [[ -z "$SILICONFLOW_API_KEY" ]] || [[ "$SILICONFLOW_API_KEY" == "your_api_key_here" ]]; then
        log_warning "请设置 SILICONFLOW_API_KEY 环境变量"
    fi
    
    if [[ -z "$JWT_SECRET" ]] || [[ "$JWT_SECRET" == "your_jwt_secret_here" ]]; then
        log_warning "请设置 JWT_SECRET 环境变量"
    fi
    
    log_success "环境配置完成"
}

# 停止现有服务
stop_existing_services() {
    log_info "停止现有服务..."
    
    # 停止全栈服务
    if docker compose -f docker-compose.fullstack.yml ps -q | grep -q .; then
        log_info "停止全栈服务..."
        docker compose -f docker-compose.fullstack.yml down
    fi
    
    # 停止传统服务
    if docker compose ps -q | grep -q .; then
        log_info "停止传统服务..."
        docker compose down
    fi
    
    log_success "现有服务已停止"
}

# 清理Docker资源
cleanup_docker() {
    log_info "清理Docker资源..."
    
    # 清理未使用的镜像
    docker image prune -f
    
    # 清理未使用的容器
    docker container prune -f
    
    # 清理未使用的网络
    docker network prune -f
    
    log_success "Docker资源清理完成"
}

# 构建和启动服务
build_and_start() {
    log_info "构建和启动服务..."
    
    # 构建并启动全栈服务
    log_info "构建Docker镜像..."
    docker compose -f docker-compose.fullstack.yml build --no-cache
    
    log_info "启动服务..."
    docker compose -f docker-compose.fullstack.yml up -d
    
    log_success "服务启动完成"
}

# 等待服务就绪
wait_for_services() {
    log_info "等待服务就绪..."
    
    local max_attempts=60
    local attempt=1
    
    # 等待PostgreSQL
    log_info "等待PostgreSQL启动..."
    while [[ $attempt -le $max_attempts ]]; do
        if docker exec ragone-postgres pg_isready -U ragone_user -d ragone &> /dev/null; then
            log_success "PostgreSQL 已就绪"
            break
        fi
        
        if [[ $attempt -eq $max_attempts ]]; then
            log_error "PostgreSQL 启动超时"
            exit 1
        fi
        
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    # 等待Redis
    log_info "等待Redis启动..."
    attempt=1
    while [[ $attempt -le $max_attempts ]]; do
        if docker exec ragone-redis redis-cli ping &> /dev/null; then
            log_success "Redis 已就绪"
            break
        fi
        
        if [[ $attempt -eq $max_attempts ]]; then
            log_error "Redis 启动超时"
            exit 1
        fi
        
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    # 等待应用
    log_info "等待应用启动..."
    attempt=1
    while [[ $attempt -le $max_attempts ]]; do
        if docker exec ragone-app curl -f http://localhost:8080/actuator/health &> /dev/null; then
            log_success "应用已就绪"
            break
        fi
        
        if [[ $attempt -eq $max_attempts ]]; then
            log_error "应用启动超时"
            exit 1
        fi
        
        echo -n "."
        sleep 3
        ((attempt++))
    done
}

# 初始化数据库
initialize_database() {
    log_info "初始化数据库..."
    
    # 检查数据库表是否存在
    local table_count=$(docker exec ragone-postgres psql -U ragone_user -d ragone -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | tr -d ' ')
    
    if [[ $table_count -lt 9 ]]; then
        log_info "执行数据库初始化脚本..."
        docker exec -i ragone-postgres psql -U ragone_user -d ragone < data/ragone_schema.sql
        
        # 验证表创建
        local new_table_count=$(docker exec ragone-postgres psql -U ragone_user -d ragone -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | tr -d ' ')
        
        if [[ $new_table_count -ge 9 ]]; then
            log_success "数据库初始化完成 (${new_table_count} 个表)"
        else
            log_warning "数据库初始化可能不完整"
        fi
    else
        log_success "数据库已初始化 (${table_count} 个表)"
    fi
}

# 验证部署
verify_deployment() {
    log_info "验证部署..."
    
    # 检查容器状态
    log_info "检查容器状态..."
    docker compose -f docker-compose.fullstack.yml ps
    
    # 检查应用健康状态
    log_info "检查应用健康状态..."
    local health_status=$(docker exec ragone-app curl -s http://localhost:8080/actuator/health | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
    
    if [[ "$health_status" == "UP" ]]; then
        log_success "应用健康检查通过"
    else
        log_warning "应用健康检查失败: $health_status"
    fi
    
    # 检查前端访问
    log_info "检查前端访问..."
    local frontend_status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 || echo "000")
    
    if [[ "$frontend_status" == "200" ]]; then
        log_success "前端访问正常"
    else
        log_warning "前端访问异常: HTTP $frontend_status"
    fi
    
    # 检查数据库连接
    log_info "检查数据库连接..."
    if docker exec ragone-postgres psql -U ragone_user -d ragone -c "SELECT 1;" &> /dev/null; then
        log_success "数据库连接正常"
    else
        log_warning "数据库连接异常"
    fi
    
    # 检查Redis连接
    log_info "检查Redis连接..."
    if docker exec ragone-redis redis-cli ping | grep -q "PONG"; then
        log_success "Redis连接正常"
    else
        log_warning "Redis连接异常"
    fi
}

# 显示部署信息
show_deployment_info() {
    log_info "部署信息:"
    echo ""
    echo "🌐 前端访问地址: http://localhost:8080"
    echo "🔧 后端API地址: http://localhost:8080/api"
    echo "📊 健康检查: http://localhost:8080/actuator/health"
    echo ""
    echo "🗄️  数据库信息:"
    echo "   - 主机: localhost"
    echo "   - 端口: 5433"
    echo "   - 数据库: ragone"
    echo "   - 用户: ragone_user"
    echo ""
    echo "🔴 Redis信息:"
    echo "   - 主机: localhost"
    echo "   - 端口: 6380"
    echo ""
    echo "📋 常用命令:"
    echo "   - 查看日志: docker logs ragone-app"
    echo "   - 重启服务: docker-compose -f docker-compose.fullstack.yml restart"
    echo "   - 停止服务: docker-compose -f docker-compose.fullstack.yml down"
    echo "   - 查看状态: docker-compose -f docker-compose.fullstack.yml ps"
    echo ""
}

# 主函数
main() {
    echo "=========================================="
    echo "    RAG-one 完整项目部署脚本"
    echo "=========================================="
    echo ""
    
    # 检查参数
    if [[ "$1" == "--help" ]] || [[ "$1" == "-h" ]]; then
        echo "用法: $0 [选项]"
        echo ""
        echo "选项:"
        echo "  --help, -h     显示帮助信息"
        echo "  --clean        清理Docker资源"
        echo "  --no-cleanup   跳过清理步骤"
        echo ""
        exit 0
    fi
    
    local skip_cleanup=false
    if [[ "$1" == "--no-cleanup" ]]; then
        skip_cleanup=true
    fi
    
    # 执行部署步骤
    check_requirements
    check_files
    setup_environment
    stop_existing_services
    
    if [[ "$1" == "--clean" ]]; then
        cleanup_docker
    elif [[ "$skip_cleanup" == false ]]; then
        log_info "跳过Docker资源清理 (使用 --clean 参数进行清理)"
    fi
    
    build_and_start
    wait_for_services
    initialize_database
    verify_deployment
    show_deployment_info
    
    echo ""
    log_success "🎉 部署完成！"
    echo ""
}

# 错误处理
trap 'log_error "部署过程中发生错误，请检查日志"; exit 1' ERR

# 执行主函数
main "$@"
