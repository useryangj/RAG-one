import React, { useEffect, useState } from 'react';
import {
  Card,
  Button,
  List,
  Avatar,
  Tag,
  Space,
  Modal,
  Form,
  Input,
  message,
  Popconfirm,
  Typography,
  Empty,
} from 'antd';
import {
  DatabaseOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  MessageOutlined,
  FileTextOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { KnowledgeBase, CreateKnowledgeBaseRequest } from '../../types';
import { knowledgeBaseApi } from '../../services/api';
import dayjs from 'dayjs';

const { Title, Text } = Typography;
const { TextArea } = Input;

const KnowledgeBaseList: React.FC = () => {
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingKb, setEditingKb] = useState<KnowledgeBase | null>(null);
  const [form] = Form.useForm();
  const navigate = useNavigate();

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

  const handleCreate = () => {
    setEditingKb(null);
    setModalVisible(true);
    form.resetFields();
  };

  const handleEdit = (kb: KnowledgeBase) => {
    setEditingKb(kb);
    setModalVisible(true);
    form.setFieldsValue({
      name: kb.name,
      description: kb.description,
    });
  };

  const handleDelete = async (id: number) => {
    try {
      await knowledgeBaseApi.delete(id);
      message.success('知识库删除成功');
      fetchKnowledgeBases();
    } catch (error) {
      console.error('Failed to delete knowledge base:', error);
    }
  };

  const handleSubmit = async (values: CreateKnowledgeBaseRequest) => {
    try {
      if (editingKb) {
        await knowledgeBaseApi.update(editingKb.id, values);
        message.success('知识库更新成功');
      } else {
        await knowledgeBaseApi.create(values);
        message.success('知识库创建成功');
      }
      setModalVisible(false);
      fetchKnowledgeBases();
    } catch (error) {
      console.error('Failed to save knowledge base:', error);
    }
  };

  const handleView = (id: number) => {
    navigate(`/knowledge-bases/${id}`);
  };

  const handleManageDocuments = (id: number) => {
    navigate(`/documents?kb=${id}`);
  };

  const handleStartChat = (id: number) => {
    navigate(`/chat?kb=${id}`);
  };

  return (
    <div>
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={2}>知识库管理</Title>
          <Text type="secondary">管理您的知识库，组织和检索文档内容</Text>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleCreate}
        >
          创建知识库
        </Button>
      </div>

      <Card>
        {knowledgeBases.length === 0 ? (
          <Empty
            description="暂无知识库"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              创建第一个知识库
            </Button>
          </Empty>
        ) : (
          <List
            loading={loading}
            dataSource={knowledgeBases}
            renderItem={(item) => (
              <List.Item
                actions={[
                  <Button
                    key="view"
                    type="text"
                    icon={<EyeOutlined />}
                    onClick={() => handleView(item.id)}
                  >
                    查看
                  </Button>,
                  <Button
                    key="documents"
                    type="text"
                    icon={<FileTextOutlined />}
                    onClick={() => handleManageDocuments(item.id)}
                  >
                    文档
                  </Button>,
                  <Button
                    key="chat"
                    type="text"
                    icon={<MessageOutlined />}
                    onClick={() => handleStartChat(item.id)}
                  >
                    对话
                  </Button>,
                  <Button
                    key="edit"
                    type="text"
                    icon={<EditOutlined />}
                    onClick={() => handleEdit(item)}
                  >
                    编辑
                  </Button>,
                  <Popconfirm
                    key="delete"
                    title="确定要删除这个知识库吗？"
                    description="删除后将无法恢复，包括其中的所有文档。"
                    onConfirm={() => handleDelete(item.id)}
                    okText="确定"
                    cancelText="取消"
                  >
                    <Button
                      type="text"
                      danger
                      icon={<DeleteOutlined />}
                    >
                      删除
                    </Button>
                  </Popconfirm>,
                ]}
              >
                <List.Item.Meta
                  avatar={
                    <Avatar
                      size={64}
                      icon={<DatabaseOutlined />}
                      style={{ backgroundColor: '#1890ff' }}
                    />
                  }
                  title={
                    <Space>
                      <span style={{ fontSize: '16px', fontWeight: 500 }}>
                        {item.name}
                      </span>
                      <Tag color="blue">{item.documentCount} 个文档</Tag>
                    </Space>
                  }
                  description={
                    <div>
                      <Text type="secondary" style={{ marginBottom: 8, display: 'block' }}>
                        {item.description || '暂无描述'}
                      </Text>
                      <Space>
                        <Text type="secondary" style={{ fontSize: '12px' }}>
                          创建于 {dayjs(item.createdAt).format('YYYY-MM-DD HH:mm')}
                        </Text>
                        <Text type="secondary" style={{ fontSize: '12px' }}>
                          更新于 {dayjs(item.updatedAt).format('YYYY-MM-DD HH:mm')}
                        </Text>
                      </Space>
                    </div>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </Card>

      {/* 创建/编辑知识库弹窗 */}
      <Modal
        title={editingKb ? '编辑知识库' : '创建知识库'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <Form.Item
            name="name"
            label="知识库名称"
            rules={[
              { required: true, message: '请输入知识库名称' },
              { min: 2, message: '名称至少2个字符' },
              { max: 50, message: '名称最多50个字符' },
            ]}
          >
            <Input placeholder="请输入知识库名称" />
          </Form.Item>

          <Form.Item
            name="description"
            label="描述"
            rules={[
              { max: 200, message: '描述最多200个字符' },
            ]}
          >
            <TextArea
              rows={4}
              placeholder="请输入知识库描述（可选）"
              showCount
              maxLength={200}
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setModalVisible(false)}>
                取消
              </Button>
              <Button type="primary" htmlType="submit">
                {editingKb ? '更新' : '创建'}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default KnowledgeBaseList;
