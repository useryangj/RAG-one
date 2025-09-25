import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, Tabs, message, Spin } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, ContactsOutlined } from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { LoginRequest, RegisterRequest } from '../types';
import './Login.css';

const { Title, Text } = Typography;
const { TabPane } = Tabs;

const Login: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('login');
  const navigate = useNavigate();
  const location = useLocation();
  const { login, register, isAuthenticated, loading: authLoading } = useAuth();

  // 获取重定向路径
  const from = (location.state as any)?.from?.pathname || '/dashboard';

  // 如果已经登录，重定向到目标页面
  React.useEffect(() => {
    if (!authLoading && isAuthenticated) {
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, authLoading, navigate, from]);

  const handleLogin = async (values: LoginRequest) => {
    setLoading(true);
    try {
      const success = await login(values);
      if (success) {
        navigate(from, { replace: true });
      }
    } catch (error) {
      console.error('Login failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (values: RegisterRequest & { confirmPassword: string }) => {
    if (values.password !== values.confirmPassword) {
      message.error('两次输入的密码不一致');
      return;
    }

    setLoading(true);
    try {
      const { confirmPassword, ...registerData } = values;
      const success = await register(registerData);
      if (success) {
        setActiveTab('login');
        message.success('注册成功！请登录');
      }
    } catch (error) {
      console.error('Register failed:', error);
    } finally {
      setLoading(false);
    }
  };

  // 在认证状态检查期间显示加载状态
  if (authLoading) {
    return (
      <div className="login-container">
        <div className="login-content">
          <Card className="login-card">
            <div style={{ textAlign: 'center', padding: '40px' }}>
              <Spin size="large" />
              <div style={{ marginTop: '16px' }}>检查登录状态...</div>
            </div>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="login-container">
      <div className="login-content">
        <Card className="login-card">
          <div className="login-header">
            <Title level={2}>RAG 智能问答系统</Title>
            <Text type="secondary">基于知识库的智能对话平台</Text>
          </div>

          <Tabs activeKey={activeTab} onChange={setActiveTab} centered>
            <TabPane tab="登录" key="login">
              <Form
                name="login"
                onFinish={handleLogin}
                autoComplete="off"
                size="large"
              >
                <Form.Item
                  name="username"
                  rules={[
                    { required: true, message: '请输入用户名' },
                    { min: 3, message: '用户名至少3个字符' }
                  ]}
                >
                  <Input
                    prefix={<UserOutlined />}
                    placeholder="用户名"
                    autoComplete="username"
                  />
                </Form.Item>

                <Form.Item
                  name="password"
                  rules={[
                    { required: true, message: '请输入密码' },
                    { min: 6, message: '密码至少6个字符' }
                  ]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="密码"
                    autoComplete="current-password"
                  />
                </Form.Item>

                <Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                    block
                  >
                    登录
                  </Button>
                </Form.Item>
              </Form>
            </TabPane>

            <TabPane tab="注册" key="register">
              <Form
                name="register"
                onFinish={handleRegister}
                autoComplete="off"
                size="large"
              >
                <Form.Item
                  name="username"
                  rules={[
                    { required: true, message: '请输入用户名' },
                    { min: 3, message: '用户名至少3个字符' },
                    { max: 20, message: '用户名最多20个字符' },
                    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线' }
                  ]}
                >
                  <Input
                    prefix={<UserOutlined />}
                    placeholder="用户名"
                    autoComplete="username"
                  />
                </Form.Item>

                <Form.Item
                  name="email"
                  rules={[
                    { required: true, message: '请输入邮箱' },
                    { type: 'email', message: '请输入有效的邮箱地址' }
                  ]}
                >
                  <Input
                    prefix={<MailOutlined />}
                    placeholder="邮箱"
                    autoComplete="email"
                  />
                </Form.Item>

                <Form.Item
                  name="fullName"
                  rules={[
                    { required: true, message: '请输入姓名' },
                    { min: 2, message: '姓名至少2个字符' },
                    { max: 50, message: '姓名最多50个字符' }
                  ]}
                >
                  <Input
                    prefix={<ContactsOutlined />}
                    placeholder="姓名"
                    autoComplete="name"
                  />
                </Form.Item>

                <Form.Item
                  name="password"
                  rules={[
                    { required: true, message: '请输入密码' },
                    { min: 6, message: '密码至少6个字符' },
                    { max: 50, message: '密码最多50个字符' }
                  ]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="密码"
                    autoComplete="new-password"
                  />
                </Form.Item>

                <Form.Item
                  name="confirmPassword"
                  dependencies={['password']}
                  rules={[
                    { required: true, message: '请确认密码' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        if (!value || getFieldValue('password') === value) {
                          return Promise.resolve();
                        }
                        return Promise.reject(new Error('两次输入的密码不一致'));
                      },
                    }),
                  ]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="确认密码"
                    autoComplete="new-password"
                  />
                </Form.Item>

                <Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                    block
                  >
                    注册
                  </Button>
                </Form.Item>
              </Form>
            </TabPane>
          </Tabs>
        </Card>
      </div>
    </div>
  );
};

export default Login;
