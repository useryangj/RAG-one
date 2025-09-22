import { User, JwtResponse } from '../types';

// Token管理
export const tokenStorage = {
  set: (token: string) => {
    localStorage.setItem('token', token);
  },
  
  get: (): string | null => {
    return localStorage.getItem('token');
  },
  
  remove: () => {
    localStorage.removeItem('token');
  },
  
  isValid: (): boolean => {
    const token = tokenStorage.get();
    if (!token) return false;
    
    try {
      // 简单的JWT token过期检查
      const payload = JSON.parse(atob(token.split('.')[1]));
      const currentTime = Date.now() / 1000;
      return payload.exp > currentTime;
    } catch {
      return false;
    }
  },
};

// 用户信息管理
export const userStorage = {
  set: (user: User) => {
    localStorage.setItem('user', JSON.stringify(user));
  },
  
  get: (): User | null => {
    const userStr = localStorage.getItem('user');
    if (!userStr) return null;
    
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  },
  
  remove: () => {
    localStorage.removeItem('user');
  },
};

// 认证工具函数
export const authUtils = {
  // 检查是否已登录
  isAuthenticated: (): boolean => {
    return tokenStorage.isValid() && userStorage.get() !== null;
  },
  
  // 登录处理
  login: (jwtResponse: JwtResponse) => {
    tokenStorage.set(jwtResponse.token);
    
    const user: User = {
      id: jwtResponse.id,
      username: jwtResponse.username,
      email: jwtResponse.email,
      fullName: jwtResponse.fullName,
      role: jwtResponse.roles.includes('ADMIN') ? 'ADMIN' : 'USER',
      createdAt: new Date().toISOString(),
    };
    
    userStorage.set(user);
  },
  
  // 登出处理
  logout: () => {
    tokenStorage.remove();
    userStorage.remove();
  },
  
  // 获取当前用户
  getCurrentUser: (): User | null => {
    return userStorage.get();
  },
  
  // 检查用户角色
  hasRole: (role: 'USER' | 'ADMIN'): boolean => {
    const user = userStorage.get();
    if (!user) return false;
    
    return user.role === role || user.role === 'ADMIN';
  },
};
