# RAG 智能问答系统

一个基于 Spring Boot + React + TypeScript 开发的 RAG (Retrieval-Augmented Generation) 智能问答系统，支持文档上传、知识库管理和基于知识库的智能对话。

## 🌟 项目特性

### 后端特性 (Spring Boot)
- **用户认证**: JWT Token 认证，支持用户注册和登录
- **知识库管理**: 创建、编辑、删除知识库，支持多用户隔离
- **文档处理**: 支持 PDF、Word、TXT 文档上传和解析
- **向量存储**: 使用 PostgreSQL + pgvector 存储文档向量
- **RAG 问答**: 集成 LangChain4j，支持基于知识库的智能问答
- **API 设计**: RESTful API，完整的错误处理和日志记录

### 前端特性 (React + TypeScript)
- **现代化 UI**: 基于 Ant Design 的美观界面
- **响应式设计**: 支持桌面端和移动端
- **文件上传**: 拖拽上传，支持多种文件格式
- **实时聊天**: 类 ChatGPT 的对话界面
- **状态管理**: React Context + Hooks
- **类型安全**: 完整的 TypeScript 类型定义

## 🏗️ 技术架构

### 后端技术栈
- **框架**: Spring Boot 3.5.6
- **安全**: Spring Security + JWT
- **数据库**: PostgreSQL + pgvector
- **ORM**: Spring Data JPA
- **AI集成**: LangChain4j
- **文档解析**: Apache Tika
- **构建工具**: Maven

### 前端技术栈
- **框架**: React 19 + TypeScript
- **UI库**: Ant Design 5.x
- **路由**: React Router v7
- **HTTP客户端**: Axios
- **文件上传**: React Dropzone
- **构建工具**: Create React App

### 数据库设计
```sql
-- 用户表
users (id, username, email, password, full_name, role, created_at, updated_at)

-- 知识库表
knowledge_bases (id, name, description, user_id, created_at, updated_at)

-- 文档表
documents (id, file_name, file_path, file_size, content_type, process_status, knowledge_base_id, user_id, uploaded_at, processed_at)

-- 文档片段表
document_chunks (id, content, embedding, document_id, chunk_index, created_at)

-- 聊天历史表
chat_history (id, question, answer, knowledge_base_id, user_id, created_at)
```

## 🚀 快速开始

### 环境要求
- **Java**: 17+
- **Node.js**: 18+
- **PostgreSQL**: 14+ (需要 pgvector 扩展)
- **Maven**: 3.8+
- **npm**: 9+

### 1. 数据库配置

#### 安装 PostgreSQL 和 pgvector
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install postgresql postgresql-contrib

# 安装 pgvector 扩展
sudo apt install postgresql-14-pgvector

# 启动 PostgreSQL
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

#### 创建数据库
```sql
-- 连接到 PostgreSQL
sudo -u postgres psql

-- 创建数据库和用户
CREATE DATABASE ragone;
CREATE USER ragone_user WITH PASSWORD 'ragone_password';
GRANT ALL PRIVILEGES ON DATABASE ragone TO ragone_user;

-- 连接到 ragone 数据库
\c ragone

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. 一键启动开发环境

我们提供了便捷的启动脚本：

```bash
# 克隆项目
git clone <repository-url>
cd RAG-one

# 一键启动前后端服务
./start-dev.sh
```

启动成功后：
- 前端地址: http://localhost:3000
- 后端地址: http://localhost:8080/api

### 3. 手动启动（可选）

#### 启动后端
```bash
# 在项目根目录
./mvnw spring-boot:run
```

#### 启动前端
```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm start
```

### 4. 停止服务
```bash
./stop-dev.sh
```

## 📱 使用说明

### 1. 用户注册和登录
- 访问 http://localhost:3000
- 点击"注册"创建新账户
- 使用用户名和密码登录系统

### 2. 创建知识库
- 登录后进入"知识库管理"
- 点击"创建知识库"
- 输入知识库名称和描述

### 3. 上传文档
- 进入"文档管理"
- 选择目标知识库
- 拖拽或点击上传文档（支持 PDF、Word、TXT、Markdown）
- 等待文档处理完成

### 4. 开始对话
- 进入"AI 对话"
- 选择知识库
- 输入问题开始智能对话

## 🔧 配置说明

### 后端配置 (application.yml)
```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ragone
    username: ragone_user
    password: ragone_password

# AI 模型配置
langchain4j:
  open-ai:
    chat-model:
      api-key: your-api-key
      base-url: https://api.siliconflow.cn/v1
      model-name: Qwen/Qwen2.5-7B-Instruct
```

### 前端配置
在 `frontend` 目录创建 `.env` 文件：
```env
REACT_APP_API_BASE_URL=http://localhost:8080/api
```

## 📂 项目结构

```
RAG-one/
├── src/main/java/com/example/ragone/    # 后端源码
│   ├── config/          # 配置类
│   ├── controller/      # 控制器
│   ├── dto/            # 数据传输对象
│   ├── entity/         # 实体类
│   ├── repository/     # 数据访问层
│   ├── security/       # 安全配置
│   └── service/        # 业务逻辑层
├── frontend/           # 前端源码
│   ├── src/
│   │   ├── components/ # React 组件
│   │   ├── contexts/   # React Context
│   │   ├── pages/      # 页面组件
│   │   ├── services/   # API 服务
│   │   ├── types/      # TypeScript 类型
│   │   └── utils/      # 工具函数
│   └── public/         # 静态资源
├── logs/               # 日志文件
├── uploads/            # 上传的文件
├── start-dev.sh        # 启动脚本
├── stop-dev.sh         # 停止脚本
└── README.md           # 项目说明
```

## 🐛 故障排除

### 常见问题

1. **数据库连接失败**
   ```bash
   # 检查 PostgreSQL 状态
   sudo systemctl status postgresql
   
   # 检查端口是否被占用
   sudo netstat -tlnp | grep 5432
   ```

2. **pgvector 扩展未安装**
   ```sql
   -- 检查扩展是否安装
   SELECT * FROM pg_extension WHERE extname = 'vector';
   
   -- 如果未安装，执行
   CREATE EXTENSION IF NOT EXISTS vector;
   ```

3. **前端启动失败**
   ```bash
   # 清除 node_modules 重新安装
   cd frontend
   rm -rf node_modules package-lock.json
   npm install
   ```

4. **API 调用失败**
   - 检查后端服务是否启动 (http://localhost:8080/api/auth/me)
   - 检查 CORS 配置
   - 查看浏览器开发者工具的网络请求

### 日志查看
```bash
# 查看后端日志
tail -f logs/backend.log

# 查看前端日志
tail -f logs/frontend.log

# 查看应用日志
tail -f logs/ragone.log
```

## 🤝 贡献指南

1. Fork 本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 开源协议

本项目基于 MIT 协议开源，详见 [LICENSE](LICENSE) 文件。

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot) - 后端框架
- [React](https://reactjs.org/) - 前端框架
- [Ant Design](https://ant.design/) - UI 组件库
- [LangChain4j](https://github.com/langchain4j/langchain4j) - AI 集成框架
- [pgvector](https://github.com/pgvector/pgvector) - PostgreSQL 向量扩展

## 📞 联系我们

如有问题或建议，欢迎通过以下方式联系：

- 提交 Issue
- 发起 Discussion
- 邮箱: your-email@example.com

---

⭐ 如果这个项目对您有帮助，请给我们一个 Star！# RAG-one
