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
          window.location.href = '/login';
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

export default api;
