import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { User, LoginRequest, RegisterRequest } from '../types';
import { authApi } from '../services/api';
import { authUtils } from '../utils/auth';
import { message } from 'antd';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (data: LoginRequest) => Promise<boolean>;
  register: (data: RegisterRequest) => Promise<boolean>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // 初始化时检查登录状态
  useEffect(() => {
    const initAuth = async () => {
      try {
        // 首先检查本地存储的用户信息
        const currentUser = authUtils.getCurrentUser();
        const hasValidToken = authUtils.isAuthenticated();
        
        if (currentUser && hasValidToken) {
          // 有用户信息且token有效，设置用户状态
          setUser(currentUser);
          
          // 异步验证token，但不阻塞UI
          authApi.getCurrentUser().catch(() => {
            // Token验证失败，静默清除状态
            console.log('Token validation failed, clearing auth state');
            authUtils.logout();
            setUser(null);
          });
        } else {
          // 没有有效的认证信息，清除状态
          authUtils.logout();
          setUser(null);
        }
      } catch (error) {
        console.error('Auth initialization error:', error);
        authUtils.logout();
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  const login = async (data: LoginRequest): Promise<boolean> => {
    try {
      setLoading(true);
      const jwtResponse = await authApi.login(data);
      
      // 保存认证信息
      authUtils.login(jwtResponse);
      
      // 获取完整用户信息
      const userInfo = await authApi.getCurrentUser();
      setUser(userInfo);
      
      message.success('登录成功！');
      return true;
    } catch (error) {
      console.error('Login error:', error);
      return false;
    } finally {
      setLoading(false);
    }
  };

  const register = async (data: RegisterRequest): Promise<boolean> => {
    try {
      setLoading(true);
      await authApi.register(data);
      message.success('注册成功！请登录');
      return true;
    } catch (error) {
      console.error('Register error:', error);
      return false;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    authUtils.logout();
    setUser(null);
    message.success('已退出登录');
  };

  const value: AuthContextType = {
    user,
    loading,
    login,
    register,
    logout,
    isAuthenticated: !!user,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
