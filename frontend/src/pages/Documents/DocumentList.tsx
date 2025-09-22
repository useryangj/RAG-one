import React, { useEffect, useState } from 'react';
import {
  Card,
  Button,
  List,
  Tag,
  Space,
  Popconfirm,
  Typography,
  Empty,
  Upload,
  message,
  Progress,
  Select,
  Spin,
} from 'antd';
import {
  FileTextOutlined,
  UploadOutlined,
  DeleteOutlined,
  InboxOutlined,
  DatabaseOutlined,
} from '@ant-design/icons';
import { useDropzone } from 'react-dropzone';
import { Document, KnowledgeBase } from '../../types';
import { documentApi, knowledgeBaseApi } from '../../services/api';
import dayjs from 'dayjs';
import './DocumentList.css';

const { Title, Text } = Typography;
const { Dragger } = Upload;

const DocumentList: React.FC = () => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [selectedKbId, setSelectedKbId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    fetchKnowledgeBases();
  }, []);

  useEffect(() => {
    if (selectedKbId) {
      fetchDocuments(selectedKbId);
    }
  }, [selectedKbId]);

  const fetchKnowledgeBases = async () => {
    try {
      const data = await knowledgeBaseApi.getAll();
      setKnowledgeBases(data);
      if (data.length > 0 && !selectedKbId) {
        setSelectedKbId(data[0].id);
      }
    } catch (error) {
      console.error('Failed to fetch knowledge bases:', error);
    }
  };

  const fetchDocuments = async (kbId: number) => {
    try {
      setLoading(true);
      const data = await documentApi.getByKnowledgeBase(kbId);
      setDocuments(data);
    } catch (error) {
      console.error('Failed to fetch documents:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await documentApi.delete(id);
      message.success('文档删除成功');
      if (selectedKbId) {
        fetchDocuments(selectedKbId);
      }
    } catch (error) {
      console.error('Failed to delete document:', error);
    }
  };

  const handleUpload = async (file: File) => {
    if (!selectedKbId) {
      message.error('请先选择知识库');
      return;
    }

    try {
      setUploading(true);
      await documentApi.upload(file, selectedKbId);
      message.success('文档上传成功');
      fetchDocuments(selectedKbId);
    } catch (error) {
      console.error('Failed to upload document:', error);
    } finally {
      setUploading(false);
    }
  };

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: (acceptedFiles) => {
      acceptedFiles.forEach(handleUpload);
    },
    accept: {
      'application/pdf': ['.pdf'],
      'application/msword': ['.doc'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
      'text/plain': ['.txt'],
      'text/markdown': ['.md'],
    },
    maxSize: 50 * 1024 * 1024, // 50MB
    disabled: !selectedKbId || uploading,
  });

  const getStatusColor = (status: Document['processStatus']) => {
    switch (status) {
      case 'COMPLETED':
        return 'success';
      case 'PROCESSING':
        return 'processing';
      case 'PENDING':
        return 'default';
      case 'FAILED':
        return 'error';
      default:
        return 'default';
    }
  };

  const getStatusText = (status: Document['processStatus']) => {
    switch (status) {
      case 'COMPLETED':
        return '已完成';
      case 'PROCESSING':
        return '处理中';
      case 'PENDING':
        return '待处理';
      case 'FAILED':
        return '处理失败';
      default:
        return '未知';
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <div>
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={2}>文档管理</Title>
          <Text type="secondary">上传和管理您的文档，支持PDF、Word和文本文件</Text>
        </div>
        <Select
          style={{ width: 200 }}
          placeholder="选择知识库"
          value={selectedKbId}
          onChange={setSelectedKbId}
          loading={knowledgeBases.length === 0}
        >
          {knowledgeBases.map((kb) => (
            <Select.Option key={kb.id} value={kb.id}>
              <Space>
                <DatabaseOutlined />
                {kb.name}
              </Space>
            </Select.Option>
          ))}
        </Select>
      </div>

      {!selectedKbId ? (
        <Card>
          <Empty
            description="请先选择一个知识库"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        </Card>
      ) : (
        <>
          {/* 文件上传区域 */}
          <Card style={{ marginBottom: 24 }}>
            <div
              {...getRootProps()}
              className={`upload-area ${isDragActive ? 'drag-active' : ''} ${uploading ? 'uploading' : ''}`}
            >
              <input {...getInputProps()} />
              <div className="upload-content">
                <InboxOutlined style={{ fontSize: 48, color: '#1890ff' }} />
                <Title level={4} style={{ marginTop: 16 }}>
                  {isDragActive ? '释放文件开始上传' : '拖拽文件到此处或点击上传'}
                </Title>
                <Text type="secondary">
                  支持 PDF、Word、TXT、Markdown 文件，最大 50MB
                </Text>
                {uploading && (
                  <div style={{ marginTop: 16 }}>
                    <Spin />
                    <Text style={{ marginLeft: 8 }}>上传中...</Text>
                  </div>
                )}
              </div>
            </div>
          </Card>

          {/* 文档列表 */}
          <Card title={`文档列表 (${documents.length})`}>
            {documents.length === 0 ? (
              <Empty
                description="暂无文档"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
              >
                <Text type="secondary">
                  上传您的第一个文档开始构建知识库
                </Text>
              </Empty>
            ) : (
              <List
                loading={loading}
                dataSource={documents}
                renderItem={(item) => (
                  <List.Item
                    actions={[
                      <Popconfirm
                        key="delete"
                        title="确定要删除这个文档吗？"
                        description="删除后将无法恢复。"
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
                        <FileTextOutlined
                          style={{ fontSize: 24, color: '#1890ff' }}
                        />
                      }
                      title={
                        <Space>
                          <span>{item.originalFileName}</span>
                          <Tag color={getStatusColor(item.processStatus)}>
                            {getStatusText(item.processStatus)}
                          </Tag>
                          {item.chunkCount > 0 && (
                            <Tag color="blue">{item.chunkCount} 个片段</Tag>
                          )}
                        </Space>
                      }
                      description={
                        <div>
                          <Space>
                            <Text type="secondary">
                              {formatFileSize(item.fileSize)}
                            </Text>
                            <Text type="secondary">
                              {item.contentType}
                            </Text>
                          </Space>
                          <div style={{ marginTop: 4 }}>
                            <Text type="secondary" style={{ fontSize: '12px' }}>
                              上传于 {dayjs(item.uploadedAt).format('YYYY-MM-DD HH:mm')}
                            </Text>
                            {item.processedAt && (
                              <Text type="secondary" style={{ fontSize: '12px', marginLeft: 16 }}>
                                处理于 {dayjs(item.processedAt).format('YYYY-MM-DD HH:mm')}
                              </Text>
                            )}
                          </div>
                        </div>
                      }
                    />
                  </List.Item>
                )}
              />
            )}
          </Card>
        </>
      )}
    </div>
  );
};

export default DocumentList;
