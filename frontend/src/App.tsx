import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import MainLayout from './components/Layout/MainLayout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import KnowledgeBaseList from './pages/KnowledgeBase/KnowledgeBaseList';
import DocumentList from './pages/Documents/DocumentList';
import ChatInterface from './pages/Chat/ChatInterface';
import CharacterList from './pages/Characters/CharacterList';
import RolePlayChat from './pages/RolePlay/RolePlayChat';
import './App.css';

function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <AuthProvider>
        <Router>
          <div className="App">
            <Routes>
              {/* 公开路由 */}
              <Route path="/login" element={<Login />} />

              {/* 受保护的路由 */}
              <Route
                path="/"
                element={
                  <ProtectedRoute>
                    <MainLayout />
                  </ProtectedRoute>
                }
              >
                <Route index element={<Navigate to="/dashboard" replace />} />
                <Route path="dashboard" element={<Dashboard />} />
                <Route path="knowledge-bases" element={<KnowledgeBaseList />} />
                <Route path="documents" element={<DocumentList />} />
                <Route path="chat" element={<ChatInterface />} />
                <Route path="characters" element={<CharacterList />} />
                <Route path="roleplay" element={<RolePlayChat />} />
                <Route path="roleplay/:characterId" element={<RolePlayChat />} />
              </Route>

              {/* 404 重定向 */}
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </div>
        </Router>
      </AuthProvider>
    </ConfigProvider>
  );
}

export default App;
