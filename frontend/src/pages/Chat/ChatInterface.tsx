import React, { useState, useEffect, useRef } from 'react';
import {
  Card,
  Input,
  Button,
  List,
  Avatar,
  Typography,
  Space,
  Select,
  Empty,
  Spin,
  message,
  Tag,
} from 'antd';
import {
  SendOutlined,
  UserOutlined,
  RobotOutlined,
  DatabaseOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import { useSearchParams } from 'react-router-dom';
import { ChatMessage, KnowledgeBase, AskQuestionRequest } from '../../types';
import { knowledgeBaseApi, ragApi } from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';
import dayjs from 'dayjs';
import './ChatInterface.css';

const { Text } = Typography;
const { TextArea } = Input;

const ChatInterface: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [selectedKbId, setSelectedKbId] = useState<number | null>(null);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [searchParams] = useSearchParams();
  const { user } = useAuth();

  useEffect(() => {
    fetchKnowledgeBases();
  }, []);

  useEffect(() => {
    // 从URL参数获取知识库ID
    const kbId = searchParams.get('kb');
    if (kbId) {
      setSelectedKbId(parseInt(kbId));
    }
  }, [searchParams]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const fetchKnowledgeBases = async () => {
    try {
      setLoading(true);
      const data = await knowledgeBaseApi.getAll();
      setKnowledgeBases(data);
      if (data.length > 0 && !selectedKbId) {
        setSelectedKbId(data[0].id);
      }
    } catch (error) {
      console.error('Failed to fetch knowledge bases:', error);
    } finally {
      setLoading(false);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleSend = async () => {
    if (!inputValue.trim() || !selectedKbId || sending) {
      return;
    }

    const question = inputValue.trim();
    setInputValue('');
    setSending(true);

    // 添加用户消息
    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      content: question,
      timestamp: Date.now(),
      knowledgeBaseId: selectedKbId,
    };

    setMessages((prev) => [...prev, userMessage]);

    try {
      const request: AskQuestionRequest = {
        question,
        knowledgeBaseId: selectedKbId,
      };

      const response = await ragApi.ask(request);

      // 添加AI回复
      const assistantMessage: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: response.answer,
        timestamp: response.timestamp,
        knowledgeBaseId: selectedKbId,
      };

      setMessages((prev) => [...prev, assistantMessage]);
    } catch (error) {
      console.error('Failed to send message:', error);
      
      // 添加错误消息
      const errorMessage: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: '抱歉，我暂时无法回答您的问题。请稍后再试。',
        timestamp: Date.now(),
        knowledgeBaseId: selectedKbId,
      };

      setMessages((prev) => [...prev, errorMessage]);
    } finally {
      setSending(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleClearChat = () => {
    setMessages([]);
  };

  const selectedKb = knowledgeBases.find((kb) => kb.id === selectedKbId);

  return (
    <div className="chat-container">
      <div className="chat-header">
        <Space>
          <Select
            style={{ width: 300 }}
            placeholder="选择知识库"
            value={selectedKbId}
            onChange={setSelectedKbId}
            loading={loading}
          >
            {knowledgeBases.map((kb) => (
              <Select.Option key={kb.id} value={kb.id}>
                <Space>
                  <DatabaseOutlined />
                  {kb.name}
                  <Tag color="blue">{kb.documentCount} 个文档</Tag>
                </Space>
              </Select.Option>
            ))}
          </Select>
          
          {messages.length > 0 && (
            <Button onClick={handleClearChat}>
              清空对话
            </Button>
          )}
        </Space>
      </div>

      <Card className="chat-card">
        {!selectedKbId ? (
          <Empty
            description="请先选择一个知识库开始对话"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        ) : selectedKb?.documentCount === 0 ? (
          <Empty
            description="该知识库还没有文档，请先上传文档"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        ) : (
          <>
            <div className="chat-messages">
              {messages.length === 0 ? (
                <div className="welcome-message">
                  <div className="welcome-content">
                    <RobotOutlined style={{ fontSize: 48, color: '#1890ff' }} />
                    <h3>欢迎使用 RAG 智能问答</h3>
                    <p>基于知识库 "{selectedKb?.name}" 的内容，我可以帮您回答相关问题。</p>
                    <div className="example-questions">
                      <Text type="secondary">您可以尝试问我：</Text>
                      <ul>
                        <li>文档中的主要内容是什么？</li>
                        <li>关于某个概念的详细解释</li>
                        <li>相关的数据和统计信息</li>
                      </ul>
                    </div>
                  </div>
                </div>
              ) : (
                <List
                  dataSource={messages}
                  renderItem={(message) => (
                    <List.Item className={`message-item ${message.role}`}>
                      <div className="message-content">
                        <Avatar
                          icon={message.role === 'user' ? <UserOutlined /> : <RobotOutlined />}
                          style={{
                            backgroundColor: message.role === 'user' ? '#1890ff' : '#52c41a',
                          }}
                        />
                        <div className="message-bubble">
                          <div className="message-text">{message.content}</div>
                          <div className="message-time">
                            <ClockCircleOutlined />
                            <Text type="secondary" style={{ fontSize: '12px', marginLeft: 4 }}>
                              {dayjs(message.timestamp).format('HH:mm:ss')}
                            </Text>
                          </div>
                        </div>
                      </div>
                    </List.Item>
                  )}
                />
              )}
              
              {sending && (
                <div className="typing-indicator">
                  <Avatar icon={<RobotOutlined />} style={{ backgroundColor: '#52c41a' }} />
                  <div className="typing-content">
                    <Spin size="small" />
                    <Text type="secondary" style={{ marginLeft: 8 }}>
                      AI 正在思考...
                    </Text>
                  </div>
                </div>
              )}
              
              <div ref={messagesEndRef} />
            </div>

            <div className="chat-input">
              <div className="input-container">
                <TextArea
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  onKeyPress={handleKeyPress}
                  placeholder="输入您的问题... (Shift+Enter 换行，Enter 发送)"
                  autoSize={{ minRows: 1, maxRows: 4 }}
                  disabled={sending}
                />
                <Button
                  type="primary"
                  icon={<SendOutlined />}
                  onClick={handleSend}
                  loading={sending}
                  disabled={!inputValue.trim() || !selectedKbId}
                  className="send-button"
                >
                  发送
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>
    </div>
  );
};

export default ChatInterface;
