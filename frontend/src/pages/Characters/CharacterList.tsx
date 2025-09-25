import React, { useState, useEffect } from 'react';
import {
  Card,
  Button,
  Table,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  Switch,
  message,
  Popconfirm,
  Avatar,
  Tooltip,
  Row,
  Col,
  Statistic,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
  StopOutlined,
  ReloadOutlined,
  UserOutlined,
  RobotOutlined,
  ThunderboltOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { ColumnsType } from 'antd/es/table';
import { Character, CreateCharacterRequest, UpdateCharacterRequest, KnowledgeBase } from '../../types';
import { characterApi, knowledgeBaseApi } from '../../services/api';
import './CharacterList.css';

const { Option } = Select;
const { TextArea } = Input;

const CharacterList: React.FC = () => {
  const [characters, setCharacters] = useState<Character[]>([]);
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingCharacter, setEditingCharacter] = useState<Character | null>(null);
  const [form] = Form.useForm();
  const [selectedKnowledgeBase, setSelectedKnowledgeBase] = useState<number | undefined>();

  // 加载数据
  const loadCharacters = async () => {
    try {
      setLoading(true);
      const data = await characterApi.getAll();
      setCharacters(data);
    } catch (error) {
      message.error('加载角色列表失败');
    } finally {
      setLoading(false);
    }
  };

  const loadKnowledgeBases = async () => {
    try {
      const data = await knowledgeBaseApi.getAll();
      setKnowledgeBases(data);
    } catch (error) {
      message.error('加载知识库列表失败');
    }
  };

  useEffect(() => {
    loadCharacters();
    loadKnowledgeBases();
  }, []);

  // 根据知识库筛选角色
  const handleKnowledgeBaseFilter = async (knowledgeBaseId?: number) => {
    setSelectedKnowledgeBase(knowledgeBaseId);
    try {
      setLoading(true);
      const data = knowledgeBaseId 
        ? await characterApi.getByKnowledgeBase(knowledgeBaseId)
        : await characterApi.getAll();
      setCharacters(data);
    } catch (error) {
      message.error('筛选角色失败');
    } finally {
      setLoading(false);
    }
  };

  // 打开创建/编辑模态框
  const handleOpenModal = (character?: Character) => {
    setEditingCharacter(character || null);
    setModalVisible(true);
    if (character) {
      form.setFieldsValue({
        name: character.name,
        description: character.description,
        avatarUrl: character.avatarUrl,
        knowledgeBaseId: character.knowledgeBaseId,
        isPublic: character.isPublic,
      });
    } else {
      form.resetFields();
    }
  };

  // 关闭模态框
  const handleCloseModal = () => {
    setModalVisible(false);
    setEditingCharacter(null);
    form.resetFields();
  };

  // 提交表单
  const handleSubmit = async (values: any) => {
    try {
      if (editingCharacter) {
        // 更新角色
        const updateData: UpdateCharacterRequest = {
          name: values.name,
          description: values.description,
          avatarUrl: values.avatarUrl,
          isPublic: values.isPublic,
        };
        await characterApi.update(editingCharacter.id, updateData);
        message.success('角色更新成功');
      } else {
        // 创建角色
        const createData: CreateCharacterRequest = {
          name: values.name,
          description: values.description,
          avatarUrl: values.avatarUrl,
          knowledgeBaseId: values.knowledgeBaseId,
          isPublic: values.isPublic || false,
        };
        await characterApi.create(createData);
        message.success('角色创建成功');
      }
      handleCloseModal();
      loadCharacters();
    } catch (error) {
      message.error(editingCharacter ? '角色更新失败' : '角色创建失败');
    }
  };

  // 删除角色
  const handleDelete = async (id: number) => {
    try {
      await characterApi.delete(id);
      message.success('角色删除成功');
      loadCharacters();
    } catch (error) {
      message.error('角色删除失败');
    }
  };

  // 切换角色状态
  const handleToggleStatus = async (id: number) => {
    try {
      await characterApi.toggleStatus(id);
      message.success('角色状态更新成功');
      loadCharacters();
    } catch (error) {
      message.error('角色状态更新失败');
    }
  };

  // 生成角色配置文件
  const handleGenerateProfile = async (id: number) => {
    try {
      await characterApi.generateProfile(id);
      message.success('角色配置文件生成中，请稍后刷新查看');
      setTimeout(() => loadCharacters(), 3000);
    } catch (error) {
      message.error('角色配置文件生成失败');
    }
  };

  // 开始角色扮演
  const handleStartRolePlay = (character: Character) => {
    // TODO: 导航到角色扮演页面
    message.info(`即将开始与 ${character.name} 的角色扮演`);
  };

  // 表格列定义
  const columns: ColumnsType<Character> = [
    {
      title: '角色',
      key: 'character',
      render: (_, record) => (
        <Space>
          <Avatar 
            src={record.avatarUrl} 
            icon={<UserOutlined />} 
            size="large"
          />
          <div>
            <div style={{ fontWeight: 'bold' }}>{record.name}</div>
            <div style={{ color: '#666', fontSize: '12px' }}>
              {record.description}
            </div>
          </div>
        </Space>
      ),
    },
    {
      title: '知识库',
      dataIndex: 'knowledgeBaseId',
      key: 'knowledgeBase',
      render: (knowledgeBaseId) => {
        const kb = knowledgeBases.find(kb => kb.id === knowledgeBaseId);
        return kb ? kb.name : '未知知识库';
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const statusConfig = {
          ACTIVE: { color: 'green', text: '活跃' },
          INACTIVE: { color: 'red', text: '停用' },
          DRAFT: { color: 'orange', text: '草稿' },
        };
        const config = statusConfig[status as keyof typeof statusConfig];
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: '配置文件状态',
      key: 'profileStatus',
      render: (_, record) => {
        if (!record.profile) {
          return <Tag color="default">未生成</Tag>;
        }
        const statusConfig = {
          DRAFT: { color: 'orange', text: '草稿' },
          GENERATING: { color: 'blue', text: '生成中' },
          COMPLETED: { color: 'green', text: '已完成' },
          FAILED: { color: 'red', text: '生成失败' },
        };
        const config = statusConfig[record.profile.status as keyof typeof statusConfig];
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: '可见性',
      dataIndex: 'isPublic',
      key: 'isPublic',
      render: (isPublic) => (
        <Tag color={isPublic ? 'blue' : 'default'}>
          {isPublic ? '公开' : '私有'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (createdAt) => new Date(createdAt).toLocaleString(),
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Tooltip title="查看详情">
            <Button 
              type="text" 
              icon={<EyeOutlined />} 
              size="small"
              onClick={() => handleOpenModal(record)}
            />
          </Tooltip>
          <Tooltip title="编辑">
            <Button 
              type="text" 
              icon={<EditOutlined />} 
              size="small"
              onClick={() => handleOpenModal(record)}
            />
          </Tooltip>
          <Tooltip title={record.status === 'ACTIVE' ? '停用' : '激活'}>
            <Button 
              type="text" 
              icon={record.status === 'ACTIVE' ? <StopOutlined /> : <PlayCircleOutlined />}
              size="small"
              onClick={() => handleToggleStatus(record.id)}
            />
          </Tooltip>
          {(!record.profile || record.profile.status === 'FAILED') && (
            <Tooltip title="生成配置文件">
              <Button 
                type="text" 
                icon={<ThunderboltOutlined />} 
                size="small"
                onClick={() => handleGenerateProfile(record.id)}
              />
            </Tooltip>
          )}
          {record.status === 'ACTIVE' && record.profile?.status === 'COMPLETED' && (
            <Tooltip title="开始角色扮演">
              <Button 
                type="text" 
                icon={<RobotOutlined />} 
                size="small"
                onClick={() => handleStartRolePlay(record)}
              />
            </Tooltip>
          )}
          <Popconfirm
            title="确定要删除这个角色吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Tooltip title="删除">
              <Button 
                type="text" 
                icon={<DeleteOutlined />} 
                size="small"
                danger
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // 统计数据
  const stats = {
    total: characters.length,
    active: characters.filter(c => c.status === 'ACTIVE').length,
    withProfile: characters.filter(c => c.profile?.status === 'COMPLETED').length,
    public: characters.filter(c => c.isPublic).length,
  };

  return (
    <div className="character-list">
      {/* 统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic title="总角色数" value={stats.total} prefix={<UserOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="活跃角色" value={stats.active} prefix={<PlayCircleOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="已配置角色" value={stats.withProfile} prefix={<ThunderboltOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="公开角色" value={stats.public} prefix={<EyeOutlined />} />
          </Card>
        </Col>
      </Row>

      {/* 主要内容卡片 */}
      <Card 
        title="角色管理" 
        extra={
          <Space>
            <Select
              placeholder="选择知识库筛选"
              allowClear
              style={{ width: 200 }}
              value={selectedKnowledgeBase}
              onChange={handleKnowledgeBaseFilter}
            >
              {knowledgeBases.map(kb => (
                <Option key={kb.id} value={kb.id}>{kb.name}</Option>
              ))}
            </Select>
            <Button 
              icon={<ReloadOutlined />} 
              onClick={loadCharacters}
            >
              刷新
            </Button>
            <Button 
              type="primary" 
              icon={<PlusOutlined />} 
              onClick={() => handleOpenModal()}
            >
              创建角色
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={characters}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 个角色`,
          }}
        />
      </Card>

      {/* 创建/编辑模态框 */}
      <Modal
        title={editingCharacter ? '编辑角色' : '创建角色'}
        open={modalVisible}
        onCancel={handleCloseModal}
        footer={null}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <Form.Item
            name="name"
            label="角色名称"
            rules={[{ required: true, message: '请输入角色名称' }]}
          >
            <Input placeholder="请输入角色名称" />
          </Form.Item>

          <Form.Item
            name="description"
            label="角色描述"
            rules={[{ required: true, message: '请输入角色描述' }]}
          >
            <TextArea 
              rows={3} 
              placeholder="请输入角色描述" 
            />
          </Form.Item>

          <Form.Item
            name="avatarUrl"
            label="头像URL"
          >
            <Input placeholder="请输入头像URL（可选）" />
          </Form.Item>

          {!editingCharacter && (
            <Form.Item
              name="knowledgeBaseId"
              label="关联知识库"
              rules={[{ required: true, message: '请选择关联的知识库' }]}
            >
              <Select placeholder="请选择知识库">
                {knowledgeBases.map(kb => (
                  <Option key={kb.id} value={kb.id}>{kb.name}</Option>
                ))}
              </Select>
            </Form.Item>
          )}

          <Form.Item
            name="isPublic"
            label="公开设置"
            valuePropName="checked"
          >
            <Switch checkedChildren="公开" unCheckedChildren="私有" />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingCharacter ? '更新' : '创建'}
              </Button>
              <Button onClick={handleCloseModal}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default CharacterList;