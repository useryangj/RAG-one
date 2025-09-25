import React, { useState, useEffect, useRef } from 'react';
import {
  Card,
  Input,
  Button,
  Avatar,
  Space,
  Typography,
  Divider,
  Tag,
  Rate,
  Tooltip,
  Modal,
  List,
  Spin,
  message,
  Row,
  Col,
  Statistic,
} from 'antd';
import {
  SendOutlined,
  RobotOutlined,
  UserOutlined,
  ClockCircleOutlined,
  ThunderboltOutlined,
  HistoryOutlined,
  StopOutlined,
  DeleteOutlined,
  StarOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Character,
  RolePlaySession,
  RolePlayMessage,
  SendRolePlayMessageRequest,
  StartRolePlayRequest,
} from '../../types';
import { characterApi, rolePlayApi } from '../../services/api';
import './RolePlayChat.css';

const { Text, Title } = Typography;
const { TextArea } = Input;

interface RolePlayChatProps {
  characterId?: number;
  sessionId?: string;
}

const RolePlayChat: React.FC<RolePlayChatProps> = ({ characterId, sessionId }) => {
  const params = useParams();
  const navigate = useNavigate();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  
  const [character, setCharacter] = useState<Character | null>(null);
  const [session, setSession] = useState<RolePlaySession | null>(null);
  const [messages, setMessages] = useState<RolePlayMessage[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionLoading, setSessionLoading] = useState(false);
  const [sessionsVisible, setSessionsVisible] = useState(false);
  const [userSessions, setUserSessions] = useState<RolePlaySession[]>([]);
  
  const currentCharacterId = characterId || parseInt(params.characterId || '0');
  const currentSessionId = sessionId || params.sessionId;

  // 滚动到底部
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // 加载角色信息
  const loadCharacter = async () => {
    if (!currentCharacterId) return;
    try {
      const data = await characterApi.getById(currentCharacterId);
      setCharacter(data);
    } catch (error) {
      message.error('加载角色信息失败');
    }
  };

  // 开始新会话
  const startNewSession = async () => {
    if (!currentCharacterId) return;
    try {
      setSessionLoading(true);
      const request: StartRolePlayRequest = {
        characterId: currentCharacterId,
        sessionName: `与${character?.name}的对话`,
      };
      const newSession = await rolePlayApi.startSession(request);
      setSession(newSession);
      setMessages([]);
      message.success('新会话已开始');
    } catch (error) {
      message.error('开始会话失败');
    } finally {
      setSessionLoading(false);
    }
  };

  // 加载会话信息
  const loadSession = async () => {
    if (!currentSessionId) return;
    try {
      setSessionLoading(true);
      const sessionData = await rolePlayApi.getSession(currentSessionId);
      const historyData = await rolePlayApi.getSessionHistory(currentSessionId);
      setSession(sessionData);
      setMessages(historyData);
    } catch (error) {
      message.error('加载会话失败');
    } finally {
      setSessionLoading(false);
    }
  };

  // 加载用户会话列表
  const loadUserSessions = async () => {
    try {
      const data = await rolePlayApi.getSessions();
      setUserSessions(data.filter(s => s.characterId === currentCharacterId));
    } catch (error) {
      message.error('加载会话列表失败');
    }
  };

  // 发送消息
  const sendMessage = async () => {
    if (!inputMessage.trim() || !session) return;
    
    const userMessage = inputMessage.trim();
    setInputMessage('');
    setLoading(true);

    try {
      const request: SendRolePlayMessageRequest = {
        sessionId: session.id,
        message: userMessage,
      };
      
      const response = await rolePlayApi.sendMessage(request);
      
      // 添加用户消息和角色回复到消息列表
      const newMessage: RolePlayMessage = {
        id: Date.now().toString(),
        sessionId: session.id,
        userMessage: response.userMessage,
        characterResponse: response.characterResponse,
        responseTimeMs: response.responseTimeMs,
        tokenUsage: response.tokenUsage,
        usedRag: response.usedRag,
        retrievedDocumentCount: response.retrievedDocumentCount,
        conversationTurn: messages.length + 1,
        createdAt: new Date().toISOString(),
      };
      
      setMessages(prev => [...prev, newMessage]);
      
      // 更新会话信息
      setSession(prev => prev ? {
        ...prev,
        messageCount: prev.messageCount + 1,
        tokenUsage: prev.tokenUsage + response.tokenUsage,
        lastActiveAt: new Date().toISOString(),
      } : null);
      
    } catch (error) {
      message.error('发送消息失败');
    } finally {
      setLoading(false);
    }
  };

  // 为消息评分
  const rateMessage = async (messageId: string, rating: number) => {
    try {
      await rolePlayApi.rateMessage(messageId, rating);
      message.success('评分成功');
      // 更新本地消息状态
      setMessages(prev => prev.map(msg => 
        msg.id === messageId ? { ...msg, userFeedback: rating } : msg
      ));
    } catch (error) {
      message.error('评分失败');
    }
  };

  // 结束会话
  const endSession = async () => {
    if (!session) return;
    try {
      await rolePlayApi.endSession(session.id);
      message.success('会话已结束');
      setSession(prev => prev ? { ...prev, status: 'ENDED' } : null);
    } catch (error) {
      message.error('结束会话失败');
    }
  };

  // 删除会话
  const deleteSession = async (sessionId: string) => {
    try {
      await rolePlayApi.deleteSession(sessionId);
      message.success('会话已删除');
      if (session?.id === sessionId) {
        setSession(null);
        setMessages([]);
      }
      loadUserSessions();
    } catch (error) {
      message.error('删除会话失败');
    }
  };

  useEffect(() => {
    loadCharacter();
    if (currentSessionId) {
      loadSession();
    }
    loadUserSessions();
  }, [currentCharacterId, currentSessionId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 处理回车发送
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  if (!character) {
    return (
      <div className="roleplay-chat-loading">
        <Spin size="large" />
        <Text>加载角色信息中...</Text>
      </div>
    );
  }

  return (
    <div className="roleplay-chat">
      {/* 角色信息头部 */}
      <Card className="character-header">
        <Row align="middle" justify="space-between">
          <Col>
            <Space size="large">
              <Avatar 
                src={character.avatarUrl} 
                icon={<RobotOutlined />} 
                size={64}
              />
              <div>
                <Title level={3} style={{ margin: 0 }}>{character.name}</Title>
                <Text type="secondary">{character.description}</Text>
                <div style={{ marginTop: 8 }}>
                  <Tag color="blue">角色扮演</Tag>
                  {character.profile?.status === 'COMPLETED' && (
                    <Tag color="green">配置完整</Tag>
                  )}
                </div>
              </div>
            </Space>
          </Col>
          <Col>
            <Space>
              <Button 
                icon={<HistoryOutlined />}
                onClick={() => setSessionsVisible(true)}
              >
                历史会话
              </Button>
              {!session && (
                <Button 
                  type="primary" 
                  icon={<RobotOutlined />}
                  loading={sessionLoading}
                  onClick={startNewSession}
                >
                  开始对话
                </Button>
              )}
              {session && session.status === 'ACTIVE' && (
                <Button 
                  danger
                  icon={<StopOutlined />}
                  onClick={endSession}
                >
                  结束会话
                </Button>
              )}
            </Space>
          </Col>
        </Row>
      </Card>

      {/* 会话统计 */}
      {session && (
        <Card className="session-stats">
          <Row gutter={16}>
            <Col span={6}>
              <Statistic 
                title="消息数量" 
                value={session.messageCount} 
                prefix={<HistoryOutlined />} 
              />
            </Col>
            <Col span={6}>
              <Statistic 
                title="Token使用" 
                value={session.tokenUsage} 
                prefix={<ThunderboltOutlined />} 
              />
            </Col>
            <Col span={6}>
              <Statistic 
                title="会话状态" 
                value={session.status === 'ACTIVE' ? '进行中' : '已结束'} 
                valueStyle={{ color: session.status === 'ACTIVE' ? '#3f8600' : '#cf1322' }}
              />
            </Col>
            <Col span={6}>
              <Statistic 
                title="最后活动" 
                value={new Date(session.lastActiveAt).toLocaleTimeString()} 
                prefix={<ClockCircleOutlined />} 
              />
            </Col>
          </Row>
        </Card>
      )}

      {/* 对话区域 */}
      <Card className="chat-area">
        <div className="messages-container">
          {messages.length === 0 && session && (
            <div className="welcome-message">
              <Avatar 
                src={character.avatarUrl} 
                icon={<RobotOutlined />} 
                size={48}
              />
              <div className="welcome-text">
                <Text strong>你好！我是 {character.name}</Text>
                <br />
                <Text type="secondary">{character.description}</Text>
                <br />
                <Text type="secondary">有什么我可以帮助你的吗？</Text>
              </div>
            </div>
          )}
          
          {messages.map((message, index) => (
            <div key={message.id} className="message-group">
              {/* 用户消息 */}
              <div className="message user-message">
                <div className="message-content">
                  <Avatar icon={<UserOutlined />} size="small" />
                  <div className="message-bubble user-bubble">
                    <Text>{message.userMessage}</Text>
                  </div>
                </div>
              </div>
              
              {/* 角色回复 */}
              <div className="message character-message">
                <div className="message-content">
                  <Avatar 
                    src={character.avatarUrl} 
                    icon={<RobotOutlined />} 
                    size="small"
                  />
                  <div className="message-bubble character-bubble">
                    <Text>{message.characterResponse}</Text>
                    <div className="message-meta">
                      <Space size="small">
                        <Text type="secondary" style={{ fontSize: '12px' }}>
                          <ClockCircleOutlined /> {message.responseTimeMs}ms
                        </Text>
                        {message.usedRag && (
                          <Tooltip title={`检索到 ${message.retrievedDocumentCount} 个相关文档`}>
                            <Tag color="blue">
                              <FileTextOutlined /> RAG
                            </Tag>
                          </Tooltip>
                        )}
                        <Text type="secondary" style={{ fontSize: '12px' }}>
                          <ThunderboltOutlined /> {message.tokenUsage} tokens
                        </Text>
                      </Space>
                      <div className="message-rating">
                        <Rate 
                          value={message.userFeedback || 0}
                          onChange={(rating) => rateMessage(message.id, rating)}
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              {index < messages.length - 1 && <Divider style={{ margin: '16px 0' }} />}
            </div>
          ))}
          
          <div ref={messagesEndRef} />
        </div>
        
        {/* 输入区域 */}
        {session && session.status === 'ACTIVE' && (
          <div className="input-area">
            <Space.Compact style={{ width: '100%' }}>
              <TextArea
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder={`与 ${character.name} 对话...`}
                autoSize={{ minRows: 1, maxRows: 4 }}
                disabled={loading}
              />
              <Button 
                type="primary" 
                icon={<SendOutlined />}
                loading={loading}
                onClick={sendMessage}
                disabled={!inputMessage.trim()}
              >
                发送
              </Button>
            </Space.Compact>
          </div>
        )}
        
        {!session && (
          <div className="no-session">
            <Text type="secondary">请开始一个新的对话会话</Text>
          </div>
        )}
      </Card>

      {/* 历史会话模态框 */}
      <Modal
        title="历史会话"
        open={sessionsVisible}
        onCancel={() => setSessionsVisible(false)}
        footer={null}
        width={600}
      >
        <List
          dataSource={userSessions}
          renderItem={(session) => (
            <List.Item
              actions={[
                <Button 
                  type="link" 
                  onClick={() => {
                    navigate(`/roleplay/${character.id}/session/${session.id}`);
                    setSessionsVisible(false);
                  }}
                >
                  继续对话
                </Button>,
                <Button 
                  type="link" 
                  danger
                  icon={<DeleteOutlined />}
                  onClick={() => deleteSession(session.id)}
                >
                  删除
                </Button>,
              ]}
            >
              <List.Item.Meta
                title={session.name}
                description={
                  <Space>
                    <Text type="secondary">
                      {session.messageCount} 条消息
                    </Text>
                    <Text type="secondary">
                      {new Date(session.lastActiveAt).toLocaleString()}
                    </Text>
                    <Tag color={session.status === 'ACTIVE' ? 'green' : 'default'}>
                      {session.status === 'ACTIVE' ? '进行中' : '已结束'}
                    </Tag>
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      </Modal>
    </div>
  );
};

export default RolePlayChat;