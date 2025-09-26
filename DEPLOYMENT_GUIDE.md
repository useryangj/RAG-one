# RAG-one 项目公网部署指南

本指南将帮助您将 RAG-one 项目部署到公网环境。

## 🚀 部署方案概览

### 方案A：云服务器 + Docker 部署（推荐）
- **适用场景**: 中小型项目，成本可控
- **服务器要求**: 2核4GB内存，40GB SSD
- **月成本**: 约100-200元
- **优势**: 简单易用，成本低

### 方案B：云原生部署
- **适用场景**: 大型项目，高可用需求
- **平台**: 阿里云ACK、腾讯云TKE等
- **优势**: 自动扩缩容，高可用

## 📋 部署前准备

### 1. 服务器准备
```bash
# 推荐配置
CPU: 2核心
内存: 4GB
存储: 40GB SSD
带宽: 5Mbps
操作系统: Ubuntu 20.04 LTS
```

### 2. 域名准备（可选）
- 购买域名（如：your-domain.com）
- 配置DNS解析到服务器IP
- 申请SSL证书（推荐Let's Encrypt免费证书）

### 3. 必要的API密钥
- **SiliconFlow API Key**: 用于AI模型调用
- **JWT Secret**: 用于用户认证（至少32位）

## 🛠️ 快速部署步骤

### 步骤1: 服务器环境准备

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 重新登录以应用用户组更改
exit
# 重新SSH登录
```

### 步骤2: 部署项目

```bash
# 克隆项目到服务器
git clone <your-repository-url>
cd RAG-one

# 配置环境变量
cp env.example .env
nano .env  # 编辑环境变量

# 一键部署
./deploy.sh prod build  # 构建镜像
./deploy.sh prod up     # 启动服务
```

### 步骤3: 配置环境变量

编辑 `.env` 文件，设置以下关键变量：

```bash
# 数据库密码（必须修改）
DB_PASSWORD=your_very_secure_database_password

# Redis密码（建议设置）
REDIS_PASSWORD=your_secure_redis_password

# JWT密钥（必须修改，至少32位）
JWT_SECRET=your_very_secure_jwt_secret_key_at_least_32_characters_long

# AI API密钥（必须设置）
SILICONFLOW_API_KEY=your_siliconflow_api_key

# 前端API地址（修改为您的域名或IP）
REACT_APP_API_BASE_URL=https://your-domain.com/api
```

### 步骤4: 配置防火墙

```bash
# 开放必要端口
sudo ufw allow 22      # SSH
sudo ufw allow 80      # HTTP
sudo ufw allow 443     # HTTPS
sudo ufw enable
```

### 步骤5: 配置SSL证书（可选但推荐）

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx

# 申请SSL证书
sudo certbot --nginx -d your-domain.com

# 设置自动续期
sudo crontab -e
# 添加以下行：
# 0 12 * * * /usr/bin/certbot renew --quiet
```

## 🔧 详细配置说明

### 1. 生产环境配置文件

项目已包含 `application-prod.yml` 生产环境配置：
- 数据库连接池优化
- 日志级别调整
- 安全配置加强
- 性能参数优化

### 2. Docker Compose 配置

`docker-compose.yml` 包含完整的服务编排：
- PostgreSQL 数据库（带pgvector扩展）
- Redis 缓存
- Spring Boot 后端服务
- Nginx + React 前端服务

### 3. Nginx 配置

前端使用 Nginx 提供服务，配置包括：
- 静态资源缓存
- API 反向代理
- Gzip 压缩
- SSL 支持

## 📊 监控和维护

### 1. 服务状态检查

```bash
# 检查所有服务状态
./deploy.sh prod health

# 查看服务日志
./deploy.sh prod logs

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
```

### 2. 数据备份

```bash
# 自动备份
./deploy.sh prod backup

# 手动备份数据库
docker-compose exec postgres pg_dump -U ragone_user ragone > backup.sql
```

### 3. 服务管理

```bash
# 重启服务
./deploy.sh prod restart

# 停止服务
./deploy.sh prod down

# 清理资源
./deploy.sh prod cleanup
```

## 🔒 安全最佳实践

### 1. 服务器安全
- 使用SSH密钥登录，禁用密码登录
- 定期更新系统和软件包
- 配置防火墙，只开放必要端口
- 使用非root用户运行应用

### 2. 应用安全
- 使用强密码和密钥
- 启用HTTPS
- 定期备份数据
- 监控异常访问

### 3. 数据库安全
- 使用强密码
- 限制数据库访问权限
- 定期备份数据库
- 启用数据库日志审计

## 🚨 故障排除

### 常见问题

1. **服务启动失败**
   ```bash
   # 查看详细日志
   docker-compose logs backend
   
   # 检查端口占用
   sudo netstat -tlnp | grep :8080
   ```

2. **数据库连接失败**
   ```bash
   # 检查数据库状态
   docker-compose exec postgres pg_isready -U ragone_user -d ragone
   
   # 重启数据库服务
   docker-compose restart postgres
   ```

3. **前端无法访问后端API**
   ```bash
   # 检查网络连接
   docker-compose exec frontend curl http://backend:8080/api/actuator/health
   
   # 检查环境变量
   docker-compose exec frontend env | grep REACT_APP
   ```

4. **SSL证书问题**
   ```bash
   # 检查证书状态
   sudo certbot certificates
   
   # 手动续期证书
   sudo certbot renew
   ```

### 性能优化

1. **数据库优化**
   - 调整连接池大小
   - 优化查询索引
   - 定期清理日志

2. **应用优化**
   - 调整JVM参数
   - 启用缓存
   - 优化文件上传大小

3. **前端优化**
   - 启用Gzip压缩
   - 配置静态资源缓存
   - 使用CDN加速

## 📈 扩展部署

### 负载均衡部署

如需支持更高并发，可以部署多个后端实例：

```yaml
# docker-compose.yml 扩展配置
services:
  backend-1:
    # ... 后端配置
  backend-2:
    # ... 后端配置
  
  nginx:
    # 配置负载均衡
```

### 数据库集群

对于高可用需求，可以配置PostgreSQL主从复制或集群。

## 📞 技术支持

如果在部署过程中遇到问题：

1. 查看项目文档和日志
2. 检查GitHub Issues
3. 提交新的Issue描述问题
4. 联系技术支持

---

🎉 **恭喜！** 按照本指南，您的RAG-one项目应该已经成功部署到公网了！

访问地址：
- 前端: http://your-domain.com 或 http://your-server-ip
- 后端API: http://your-domain.com/api 或 http://your-server-ip:8080/api

