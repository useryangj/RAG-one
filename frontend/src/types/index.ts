// API响应类型
export interface ApiResponse<T = any> {
  data?: T;
  message?: string;
  success?: boolean;
}

// 用户相关类型
export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: 'USER' | 'ADMIN';
  createdAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
}

export interface JwtResponse {
  token: string;
  id: number;
  username: string;
  email: string;
  fullName: string;
  roles: string[];
}

// 知识库相关类型
export interface KnowledgeBase {
  id: number;
  name: string;
  description?: string;
  userId: number;
  createdAt: string;
  updatedAt: string;
  documentCount: number;
}

export interface CreateKnowledgeBaseRequest {
  name: string;
  description?: string;
}

// 文档相关类型
export interface Document {
  id: number;
  fileName: string;
  originalFileName: string;
  filePath: string;
  fileSize: number;
  contentType: string;
  processStatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  knowledgeBaseId: number;
  userId: number;
  uploadedAt: string;
  processedAt?: string;
  chunkCount: number;
}

export interface DocumentChunk {
  id: number;
  content: string;
  embedding: number[];
  documentId: number;
  chunkIndex: number;
  createdAt: string;
}

// RAG问答相关类型
export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
  knowledgeBaseId?: number;
}

export interface AskQuestionRequest {
  question: string;
  knowledgeBaseId: number;
}

export interface AskQuestionResponse {
  question: string;
  answer: string;
  knowledgeBaseId: number;
  responseTimeMs: number;
  timestamp: number;
}

// 通用类型
export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// 角色扮演相关类型
export interface Character {
  id: number;
  name: string;
  description: string;
  avatarUrl?: string;
  userId: number;
  knowledgeBaseId: number;
  status: 'ACTIVE' | 'INACTIVE' | 'DRAFT';
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
  profile?: CharacterProfile;
}

export interface CharacterProfile {
  id: number;
  characterId: number;
  systemPrompt: string;
  backgroundStory?: string;
  personalityTraits?: string;
  speakingStyle?: string;
  interests?: string;
  expertise?: string;
  emotionalPatterns?: string;
  conversationExamples?: any;
  restrictions?: string;
  goalsAndMotivations?: string;
  status: 'DRAFT' | 'GENERATING' | 'COMPLETED' | 'FAILED';
  generationMethod: 'AI_GENERATED' | 'MANUAL_CREATED' | 'TEMPLATE_BASED';
  generationConfig?: any;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCharacterRequest {
  name: string;
  description: string;
  avatarUrl?: string;
  knowledgeBaseId: number;
  isPublic?: boolean;
  profile?: {
    systemPrompt?: string;
    backgroundStory?: string;
    personalityTraits?: string;
    speakingStyle?: string;
    interests?: string;
    expertise?: string;
    emotionalPatterns?: string;
    restrictions?: string;
    goalsAndMotivations?: string;
    generationMethod?: 'AI_GENERATED' | 'MANUAL_CREATED' | 'TEMPLATE_BASED';
  };
}

export interface UpdateCharacterRequest {
  name?: string;
  description?: string;
  avatarUrl?: string;
  isPublic?: boolean;
  status?: 'ACTIVE' | 'INACTIVE' | 'DRAFT';
}

export interface RolePlaySession {
  id: string;
  name: string;
  userId: number;
  characterId: number;
  status: 'ACTIVE' | 'PAUSED' | 'ENDED';
  config?: any;
  messageCount: number;
  tokenUsage: number;
  lastActiveAt: string;
  createdAt: string;
  updatedAt: string;
  character?: Character;
}

export interface RolePlayMessage {
  id: string;
  sessionId: string;
  userMessage: string;
  characterResponse: string;
  contextChunks?: string;
  systemPrompt?: string;
  responseTimeMs: number;
  tokenUsage: number;
  emotionalAnalysis?: string;
  userFeedback?: number;
  conversationTurn: number;
  usedRag: boolean;
  retrievedDocumentCount: number;
  createdAt: string;
}

export interface StartRolePlayRequest {
  characterId: number;
  sessionName?: string;
}

export interface SendRolePlayMessageRequest {
  sessionId: string;
  message: string;
}

export interface RolePlayMessageResponse {
  sessionId: string;
  userMessage: string;
  characterResponse: string;
  responseTimeMs: number;
  tokenUsage: number;
  usedRag: boolean;
  retrievedDocumentCount: number;
  timestamp: number;
}
