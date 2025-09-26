#!/bin/bash

# 测试认证和角色扮演API的脚本

BASE_URL="http://localhost:8080/api"

echo "🔐 测试用户认证和角色扮演API"
echo "================================"

# 1. 用户登录
echo "1. 尝试用户登录..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }')

echo "登录响应: $LOGIN_RESPONSE"

# 检查登录是否成功
if echo "$LOGIN_RESPONSE" | grep -q "token"; then
    echo "✅ 登录成功"
    
    # 提取JWT token
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "JWT Token: ${TOKEN:0:50}..."
    
    # 2. 测试角色扮演API
    echo ""
    echo "2. 测试角色扮演API..."
    
    # 首先获取可用角色列表
    echo "获取角色列表..."
    CHARACTERS_RESPONSE=$(curl -s -X GET "$BASE_URL/characters" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json")
    
    echo "角色列表响应: $CHARACTERS_RESPONSE"
    
    # 如果有角色，尝试开始角色扮演会话
    if echo "$CHARACTERS_RESPONSE" | grep -q '"id"'; then
        # 提取第一个角色的ID
        CHARACTER_ID=$(echo "$CHARACTERS_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        echo "使用角色ID: $CHARACTER_ID"
        
        echo ""
        echo "3. 开始角色扮演会话..."
        ROLEPLAY_RESPONSE=$(curl -s -X POST "$BASE_URL/roleplay/start" \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d "{
            \"characterId\": $CHARACTER_ID,
            \"sessionName\": \"测试会话\"
          }")
        
        echo "角色扮演会话响应: $ROLEPLAY_RESPONSE"
        
        if echo "$ROLEPLAY_RESPONSE" | grep -q '"id"'; then
            echo "✅ 角色扮演会话创建成功"
        else
            echo "❌ 角色扮演会话创建失败"
        fi
    else
        echo "⚠️  没有找到可用角色，创建一个测试角色..."
        
        # 首先获取知识库列表
        KB_RESPONSE=$(curl -s -X GET "$BASE_URL/knowledge-bases" \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json")
        
        if echo "$KB_RESPONSE" | grep -q '"id"'; then
            KB_ID=$(echo "$KB_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
            echo "使用知识库ID: $KB_ID"
            
            # 创建测试角色
            CREATE_CHARACTER_RESPONSE=$(curl -s -X POST "$BASE_URL/characters" \
              -H "Authorization: Bearer $TOKEN" \
              -H "Content-Type: application/json" \
              -d "{
                \"name\": \"测试助手\",
                \"description\": \"一个友好的AI助手\",
                \"knowledgeBaseId\": $KB_ID,
                \"personality\": \"友好、耐心、乐于助人\",
                \"background\": \"我是一个AI助手，专门帮助用户解答问题。\"
              }")
            
            echo "创建角色响应: $CREATE_CHARACTER_RESPONSE"
            
            if echo "$CREATE_CHARACTER_RESPONSE" | grep -q '"id"'; then
                CHARACTER_ID=$(echo "$CREATE_CHARACTER_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
                echo "✅ 角色创建成功，ID: $CHARACTER_ID"
                
                # 现在尝试开始会话
                echo ""
                echo "4. 使用新创建的角色开始会话..."
                ROLEPLAY_RESPONSE=$(curl -s -X POST "$BASE_URL/roleplay/start" \
                  -H "Authorization: Bearer $TOKEN" \
                  -H "Content-Type: application/json" \
                  -d "{
                    \"characterId\": $CHARACTER_ID,
                    \"sessionName\": \"测试会话\"
                  }")
                
                echo "角色扮演会话响应: $ROLEPLAY_RESPONSE"
                
                if echo "$ROLEPLAY_RESPONSE" | grep -q '"id"'; then
                    echo "✅ 角色扮演会话创建成功"
                else
                    echo "❌ 角色扮演会话创建失败"
                fi
            else
                echo "❌ 角色创建失败"
            fi
        else
            echo "❌ 没有找到知识库，请先创建知识库"
        fi
    fi
    
else
    echo "❌ 登录失败"
    echo "请检查用户名和密码是否正确"
fi

echo ""
echo "测试完成"

