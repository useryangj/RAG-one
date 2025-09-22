# RAG 智能问答系统 - 前端

这是一个基于 React + TypeScript 开发的 RAG (Retrieval-Augmented Generation) 智能问答系统前端应用。

## 技术栈

- **React 19** - 前端框架
- **TypeScript** - 类型安全
- **Ant Design** - UI 组件库
- **React Router** - 路由管理
- **Axios** - HTTP 客户端
- **React Dropzone** - 文件拖拽上传

## 功能特性

### 🔐 用户认证
- 用户注册和登录
- JWT Token 认证
- 路由保护

### 📚 知识库管理
- 创建、编辑、删除知识库
- 知识库列表和详情查看
- 文档数量统计

### 📄 文档管理
- 支持拖拽上传文件
- 支持 PDF、Word、TXT、Markdown 格式
- 文档处理状态跟踪
- 文档列表和删除功能

### 💬 智能对话
- 基于知识库的 AI 问答
- 实时聊天界面
- 消息历史记录
- 响应时间显示

### 📊 仪表盘
- 数据统计概览
- 快速操作入口
- 最近活动展示
- 使用指南

## 安装和运行

### 前置要求
- Node.js >= 18.0.0
- npm >= 9.0.0

### 安装依赖
```bash
npm install
```

### 启动开发服务器
```bash
npm start
```

应用将在 http://localhost:3000 启动

### 构建生产版本
```bash
npm run build
```

## 项目结构

```
src/
├── components/          # 通用组件
│   ├── Layout/         # 布局组件
│   └── ProtectedRoute.tsx
├── contexts/           # React Context
│   └── AuthContext.tsx
├── pages/              # 页面组件
│   ├── Chat/          # 聊天页面
│   ├── Documents/     # 文档管理
│   ├── KnowledgeBase/ # 知识库管理
│   ├── Dashboard.tsx  # 仪表盘
│   └── Login.tsx      # 登录页面
├── services/          # API 服务
│   └── api.ts
├── types/             # TypeScript 类型定义
│   └── index.ts
├── utils/             # 工具函数
│   └── auth.ts
├── App.tsx            # 主应用组件
└── index.tsx         # 应用入口
```

## API 集成

前端应用通过 Axios 与后端 Spring Boot API 进行通信：

- **基础 URL**: `http://localhost:8080/api`
- **认证**: Bearer Token (JWT)
- **错误处理**: 统一的拦截器处理

### 主要 API 端点

- `POST /auth/login` - 用户登录
- `POST /auth/register` - 用户注册
- `GET /auth/me` - 获取当前用户信息
- `GET /knowledge-bases` - 获取知识库列表
- `POST /knowledge-bases` - 创建知识库
- `POST /documents/upload` - 上传文档
- `POST /rag/ask` - AI 问答

## 环境配置

创建 `.env` 文件来配置环境变量：

```env
REACT_APP_API_BASE_URL=http://localhost:8080/api
REACT_APP_VERSION=1.0.0
```

## 部署

### 使用 Nginx

1. 构建生产版本：
```bash
npm run build
```

2. 将 `build` 目录下的文件部署到 Nginx 服务器

3. 配置 Nginx 反向代理：
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    location / {
        root /path/to/build;
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 开发指南

### 代码规范
- 使用 TypeScript 进行类型检查
- 遵循 ESLint 规则
- 使用 Prettier 格式化代码

### 组件开发
- 使用函数组件和 Hooks
- 合理使用 React Context 进行状态管理
- 组件职责单一，易于测试

### 样式规范
- 使用 Ant Design 组件库
- CSS Modules 或 styled-components
- 响应式设计支持移动端

## 故障排除

### 常见问题

1. **网络连接失败**
   - 检查后端服务是否启动
   - 确认 API 基础 URL 配置正确

2. **认证失败**
   - 清除浏览器本地存储
   - 检查 JWT Token 是否过期

3. **文件上传失败**
   - 检查文件大小是否超过限制
   - 确认文件格式是否支持

## 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

MIT License