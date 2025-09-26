# RAG-one 401认证错误解决方案

## 问题描述
前端请求 `http://localhost:8080/api/roleplay/start` 接口时返回401错误。

## 问题根因
1. **前端没有正确发送JWT token**：后端日志显示 `JWT token present: false`
2. **认证状态不一致**：前端可能认为用户已登录，但token实际上已过期或无效
3. **角色不存在**：请求中使用的characterId可能不存在

## 解决步骤

### 步骤1：清除前端认证状态
在浏览器开发者工具的Console中执行：
```javascript
// 清除所有本地存储
localStorage.clear();
sessionStorage.clear();
// 刷新页面
location.reload();
```

### 步骤2：重新登录
使用以下测试账号登录：
- **用户名**: `testuser`
- **密码**: `testpass123`

### 步骤3：验证认证状态
登录后，在浏览器Console中检查：
```javascript
// 检查token
const token = localStorage.getItem('token');
console.log('Token存在:', !!token);

// 检查token是否过期
if (token) {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const isExpired = payload.exp < Date.now() / 1000;
    console.log('Token过期:', isExpired);
    console.log('Token内容:', payload);
}

// 检查用户信息
const user = localStorage.getItem('user');
console.log('用户信息:', user ? JSON.parse(user) : null);
```

### 步骤4：创建测试角色
由于系统中可能没有可用的角色，需要先创建一个：

1. 登录后访问角色管理页面
2. 创建一个新角色
3. 确保角色状态为"激活"

### 步骤5：测试API调用
使用浏览器开发者工具的Network标签页监控请求：

1. 确认请求头包含 `Authorization: Bearer [token]`
2. 确认使用正确的角色ID
3. 检查响应状态和内容

## 快速测试方法

### 方法1：使用测试页面
打开项目根目录下的 `test_frontend_auth.html` 文件进行测试。

### 方法2：使用curl命令测试
```bash
# 1. 登录获取token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "testpass123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 2. 获取角色列表
curl -X GET http://localhost:8080/api/characters \
  -H "Authorization: Bearer $TOKEN"

# 3. 测试角色扮演接口（使用实际存在的角色ID）
curl -X POST http://localhost:8080/api/roleplay/start \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"characterId": 1, "sessionName": "测试会话"}'
```

## 常见问题排查

### 问题1：Token不存在
**症状**: `localStorage.getItem('token')` 返回 `null`
**解决**: 重新登录

### 问题2：Token过期
**症状**: Token存在但API仍返回401
**解决**: 清除存储并重新登录

### 问题3：角色不存在
**症状**: 认证成功但返回"角色不存在或无权限访问"
**解决**: 
1. 先获取角色列表确认可用角色
2. 使用正确的角色ID
3. 确保角色状态为ACTIVE

### 问题4：CORS错误
**症状**: 浏览器控制台显示CORS错误
**解决**: 确认后端CORS配置正确，或使用相同域名访问

## 预防措施

1. **实现token自动刷新机制**
2. **添加更好的错误处理和用户提示**
3. **在API调用前验证认证状态**
4. **添加请求拦截器确保token正确发送**

## 调试工具

项目中提供了以下调试工具：
- `debug_auth.sh`: 后端认证流程测试
- `test_frontend_auth.html`: 前端认证测试页面
- `fix_auth_issue.sh`: 自动修复脚本（需要完善）

如果问题仍然存在，请检查：
1. 后端服务是否正常运行
2. 数据库连接是否正常
3. JWT密钥配置是否正确
4. 网络连接是否正常
