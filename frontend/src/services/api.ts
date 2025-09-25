import axios, { AxiosInstance, AxiosResponse, AxiosError } from 'axios';
import { message } from 'antd';
import {
  User,
  LoginRequest,
  RegisterRequest,
  JwtResponse,
  KnowledgeBase,
  CreateKnowledgeBaseRequest,
  Document,
  AskQuestionRequest,
  AskQuestionResponse,
  ApiResponse,
  Character,
  CreateCharacterRequest,
  UpdateCharacterRequest,
  RolePlaySession,
  RolePlayMessage,
  StartRolePlayRequest,
  SendRolePlayMessageRequest,
  RolePlayMessageResponse,
} from '../types';

// 创建axios实例
const api: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器 - 添加JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器 - 统一错误处理
api.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  (error: AxiosError<ApiResponse>) => {
    if (error.response) {
      const { status, data } = error.response;
      
      switch (status) {
        case 401:
          message.error('登录已过期，请重新登录');
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          // 使用React Router进行重定向，而不是直接操作window.location
          if (window.location.pathname !== '/login') {
            window.history.pushState({}, '', '/login');
            window.dispatchEvent(new PopStateEvent('popstate'));
          }
          break;
        case 403:
          message.error('没有权限访问该资源');
          break;
        case 404:
          message.error('请求的资源不存在');
          break;
        case 500:
          message.error('服务器内部错误');
          break;
        default:
          message.error(data?.message || '请求失败');
      }
    } else if (error.request) {
      message.error('网络连接失败，请检查网络设置');
    } else {
      message.error('请求配置错误');
    }
    
    return Promise.reject(error);
  }
);

// 认证相关API
export const authApi = {
  // 用户登录
  login: async (data: LoginRequest): Promise<JwtResponse> => {
    const response = await api.post<JwtResponse>('/auth/login', data);
    return response.data;
  },

  // 用户注册
  register: async (data: RegisterRequest): Promise<ApiResponse> => {
    const response = await api.post<ApiResponse>('/auth/register', data);
    return response.data;
  },

  // 获取当前用户信息
  getCurrentUser: async (): Promise<User> => {
    const response = await api.get<User>('/auth/me');
    return response.data;
  },
};

// 知识库相关API
export const knowledgeBaseApi = {
  // 创建知识库
  create: async (data: CreateKnowledgeBaseRequest): Promise<KnowledgeBase> => {
    const formData = new FormData();
    formData.append('name', data.name);
    if (data.description) {
      formData.append('description', data.description);
    }
    
    const response = await api.post<KnowledgeBase>('/knowledge-bases', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  // 获取用户的所有知识库
  getAll: async (): Promise<KnowledgeBase[]> => {
    const response = await api.get<KnowledgeBase[]>('/knowledge-bases');
    return response.data;
  },

  // 获取知识库详情
  getById: async (id: number): Promise<KnowledgeBase> => {
    const response = await api.get<KnowledgeBase>(`/knowledge-bases/${id}`);
    return response.data;
  },

  // 更新知识库
  update: async (id: number, data: CreateKnowledgeBaseRequest): Promise<KnowledgeBase> => {
    const formData = new FormData();
    formData.append('name', data.name);
    if (data.description) {
      formData.append('description', data.description);
    }
    
    const response = await api.put<KnowledgeBase>(`/knowledge-bases/${id}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  // 删除知识库
  delete: async (id: number): Promise<ApiResponse> => {
    const response = await api.delete<ApiResponse>(`/knowledge-bases/${id}`);
    return response.data;
  },
};

// 文档相关API
export const documentApi = {
  // 上传文档
  upload: async (file: File, knowledgeBaseId: number): Promise<Document> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('knowledgeBaseId', knowledgeBaseId.toString());
    
    const response = await api.post<Document>('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  // 获取知识库的文档列表
  getByKnowledgeBase: async (knowledgeBaseId: number): Promise<Document[]> => {
    const response = await api.get<Document[]>(`/documents?knowledgeBaseId=${knowledgeBaseId}`);
    return response.data;
  },

  // 删除文档
  delete: async (id: number): Promise<ApiResponse> => {
    const response = await api.delete<ApiResponse>(`/documents/${id}`);
    return response.data;
  },
};

// RAG问答相关API
export const ragApi = {
  // 提问
  ask: async (data: AskQuestionRequest): Promise<AskQuestionResponse> => {
    const formData = new FormData();
    formData.append('question', data.question);
    formData.append('knowledgeBaseId', data.knowledgeBaseId.toString());
    
    const response = await api.post<AskQuestionResponse>('/rag/ask', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },
};

// 角色扮演相关API
export const characterApi = {
  // 创建角色
  create: async (data: CreateCharacterRequest): Promise<Character> => {
    const response = await api.post<Character>('/characters', data);
    return response.data;
  },

  // 获取用户的所有角色
  getAll: async (): Promise<Character[]> => {
    const response = await api.get<Character[]>('/characters');
    return response.data;
  },

  // 根据知识库获取角色
  getByKnowledgeBase: async (knowledgeBaseId: number): Promise<Character[]> => {
    const response = await api.get<Character[]>(`/characters/knowledge-base/${knowledgeBaseId}`);
    return response.data;
  },

  // 获取角色详情
  getById: async (id: number): Promise<Character> => {
    const response = await api.get<Character>(`/characters/${id}`);
    return response.data;
  },

  // 更新角色
  update: async (id: number, data: UpdateCharacterRequest): Promise<Character> => {
    const response = await api.put<Character>(`/characters/${id}`, data);
    return response.data;
  },

  // 删除角色
  delete: async (id: number): Promise<ApiResponse> => {
    const response = await api.delete<ApiResponse>(`/characters/${id}`);
    return response.data;
  },

  // 激活/停用角色
  toggleStatus: async (id: number): Promise<Character> => {
    const response = await api.patch<Character>(`/characters/${id}/toggle-status`);
    return response.data;
  },

  // 生成角色配置文件
  generateProfile: async (id: number): Promise<ApiResponse> => {
    const response = await api.post<ApiResponse>(`/characters/${id}/generate-profile`);
    return response.data;
  },

  // 搜索角色
  search: async (query: string, knowledgeBaseId?: number): Promise<Character[]> => {
    const params = new URLSearchParams({ query });
    if (knowledgeBaseId) {
      params.append('knowledgeBaseId', knowledgeBaseId.toString());
    }
    const response = await api.get<Character[]>(`/characters/search?${params}`);
    return response.data;
  },
};

// 角色扮演会话相关API
export const rolePlayApi = {
  // 开始角色扮演会话
  startSession: async (data: StartRolePlayRequest): Promise<RolePlaySession> => {
    const response = await api.post<RolePlaySession>('/roleplay/start', data);
    return response.data;
  },

  // 发送消息
  sendMessage: async (data: SendRolePlayMessageRequest): Promise<RolePlayMessageResponse> => {
    const response = await api.post<RolePlayMessageResponse>('/roleplay/message', data);
    return response.data;
  },

  // 获取用户的会话列表
  getSessions: async (): Promise<RolePlaySession[]> => {
    const response = await api.get<RolePlaySession[]>('/roleplay/sessions');
    return response.data;
  },

  // 获取会话详情
  getSession: async (sessionId: string): Promise<RolePlaySession> => {
    const response = await api.get<RolePlaySession>(`/roleplay/sessions/${sessionId}`);
    return response.data;
  },

  // 获取会话历史消息
  getSessionHistory: async (sessionId: string): Promise<RolePlayMessage[]> => {
    const response = await api.get<RolePlayMessage[]>(`/roleplay/sessions/${sessionId}/history`);
    return response.data;
  },

  // 结束会话
  endSession: async (sessionId: string): Promise<ApiResponse> => {
    const response = await api.patch<ApiResponse>(`/roleplay/sessions/${sessionId}/end`);
    return response.data;
  },

  // 删除会话
  deleteSession: async (sessionId: string): Promise<ApiResponse> => {
    const response = await api.delete<ApiResponse>(`/roleplay/sessions/${sessionId}`);
    return response.data;
  },

  // 为消息评分
  rateMessage: async (messageId: string, rating: number): Promise<ApiResponse> => {
    const response = await api.patch<ApiResponse>(`/roleplay/messages/${messageId}/rate`, { rating });
    return response.data;
  },
};

export default api;
