import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Typography, Button, List, Avatar, Tag, Space } from 'antd';
import {
  DatabaseOutlined,
  FileTextOutlined,
  MessageOutlined,
  PlusOutlined,
  EyeOutlined,
  DeleteOutlined,
  EditOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { KnowledgeBase } from '../types';
import { knowledgeBaseApi } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

const Dashboard: React.FC = () => {
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuth();

  useEffect(() => {
    fetchKnowledgeBases();
  }, []);

  const fetchKnowledgeBases = async () => {
    try {
      setLoading(true);
      const data = await knowledgeBaseApi.getAll();
      setKnowledgeBases(data);
    } catch (error) {
      console.error('Failed to fetch knowledge bases:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateKnowledgeBase = () => {
    navigate('/knowledge-bases/create');
  };

  const handleViewKnowledgeBase = (id: number) => {
    navigate(`/knowledge-bases/${id}`);
  };

  const handleEditKnowledgeBase = (id: number) => {
    navigate(`/knowledge-bases/${id}/edit`);
  };

  const handleStartChat = (id: number) => {
    navigate(`/chat?kb=${id}`);
  };

  const totalDocuments = knowledgeBases.reduce((sum, kb) => sum + kb.documentCount, 0);
  const recentKnowledgeBases = knowledgeBases
    .sort((a, b) => dayjs(b.updatedAt).valueOf() - dayjs(a.updatedAt).valueOf())
    .slice(0, 5);

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>仪表盘</Title>
        <Text type="secondary">
          欢迎回来，{user?.fullName || user?.username}！这里是您的工作概览。
        </Text>
      </div>

      {/* 统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="知识库总数"
              value={knowledgeBases.length}
              prefix={<DatabaseOutlined style={{ color: '#1890ff' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="文档总数"
              value={totalDocuments}
              prefix={<FileTextOutlined style={{ color: '#52c41a' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="今日对话"
              value={0}
              prefix={<MessageOutlined style={{ color: '#faad14' }} />}
            />
          </Card>
        </Col>
      </Row>

      {/* 快速操作 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={12}>
          <Card
            title="快速操作"
            extra={
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleCreateKnowledgeBase}
              >
                创建知识库
              </Button>
            }
          >
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              <Button
                size="large"
                icon={<DatabaseOutlined />}
                onClick={() => navigate('/knowledge-bases')}
                block
              >
                管理知识库
              </Button>
              <Button
                size="large"
                icon={<FileTextOutlined />}
                onClick={() => navigate('/documents')}
                block
              >
                文档管理
              </Button>
              <Button
                size="large"
                icon={<MessageOutlined />}
                onClick={() => navigate('/chat')}
                block
              >
                开始对话
              </Button>
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            title="最近的知识库"
            extra={
              <Button
                type="link"
                onClick={() => navigate('/knowledge-bases')}
              >
                查看全部
              </Button>
            }
          >
            <List
              loading={loading}
              dataSource={recentKnowledgeBases}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <Button
                      key="view"
                      type="text"
                      icon={<EyeOutlined />}
                      onClick={() => handleViewKnowledgeBase(item.id)}
                    />,
                    <Button
                      key="chat"
                      type="text"
                      icon={<MessageOutlined />}
                      onClick={() => handleStartChat(item.id)}
                    />,
                    <Button
                      key="edit"
                      type="text"
                      icon={<EditOutlined />}
                      onClick={() => handleEditKnowledgeBase(item.id)}
                    />,
                  ]}
                >
                  <List.Item.Meta
                    avatar={
                      <Avatar
                        icon={<DatabaseOutlined />}
                        style={{ backgroundColor: '#1890ff' }}
                      />
                    }
                    title={item.name}
                    description={
                      <Space>
                        <Text type="secondary">
                          {item.description || '暂无描述'}
                        </Text>
                        <Tag color="blue">{item.documentCount} 个文档</Tag>
                      </Space>
                    }
                  />
                  <div style={{ textAlign: 'right' }}>
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                      更新于 {dayjs(item.updatedAt).format('MM-DD HH:mm')}
                    </Text>
                  </div>
                </List.Item>
              )}
              locale={{ emptyText: '暂无知识库' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 使用提示 */}
      <Card title="使用指南" style={{ marginBottom: 24 }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} md={8}>
            <Card size="small" title="1. 创建知识库">
              <Text type="secondary">
                首先创建一个知识库来组织您的文档和知识内容。
              </Text>
            </Card>
          </Col>
          <Col xs={24} md={8}>
            <Card size="small" title="2. 上传文档">
              <Text type="secondary">
                上传PDF、Word或文本文件到知识库中，系统会自动处理和索引。
              </Text>
            </Card>
          </Col>
          <Col xs={24} md={8}>
            <Card size="small" title="3. 开始对话">
              <Text type="secondary">
                基于上传的文档内容，与AI进行智能问答对话。
              </Text>
            </Card>
          </Col>
        </Row>
      </Card>
    </div>
  );
};

export default Dashboard;
